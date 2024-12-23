package moe.nea.ledger.modules

import com.google.gson.JsonPrimitive
import moe.nea.ledger.DevUtil
import moe.nea.ledger.LedgerLogger
import moe.nea.ledger.TriggerCommand
import moe.nea.ledger.config.LedgerConfig
import moe.nea.ledger.config.MainOptions
import moe.nea.ledger.events.RegistrationFinishedEvent
import moe.nea.ledger.events.TriggerEvent
import moe.nea.ledger.gen.BuildConfig
import moe.nea.ledger.utils.ErrorUtil
import moe.nea.ledger.utils.MinecraftExecutor
import moe.nea.ledger.utils.di.Inject
import moe.nea.ledger.utils.network.RequestUtil
import moe.nea.libautoupdate.CurrentVersion
import moe.nea.libautoupdate.GithubReleaseUpdateData
import moe.nea.libautoupdate.GithubReleaseUpdateSource
import moe.nea.libautoupdate.PotentialUpdate
import moe.nea.libautoupdate.UpdateContext
import moe.nea.libautoupdate.UpdateData
import moe.nea.libautoupdate.UpdateTarget
import moe.nea.libautoupdate.UpdateUtils
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.CompletableFuture

class UpdateChecker @Inject constructor(
	val errorUtil: ErrorUtil,
	val requestUtil: RequestUtil,
) {

	@Inject
	lateinit var minecraftExecutor: MinecraftExecutor

	val updater = UpdateContext(
		NightlyAwareGithubUpdateSource("nea89o", "LocalTransactionLedger"),
		if (DevUtil.isDevEnv) UpdateTarget { listOf() }
		else UpdateTarget.deleteAndSaveInTheSameFolder(UpdateChecker::class.java),
		CurrentVersion.ofTag(BuildConfig.GIT_COMMIT),
		"ledger"
	)

	class NightlyAwareGithubUpdateSource(owner: String, repository: String) :
		GithubReleaseUpdateSource(owner, repository) {
		override fun selectUpdate(updateStream: String, releases: List<GithubRelease>): UpdateData? {
			if (updateStream == "nightly") {
				return findAsset(releases.find { it.tagName == "nightly" })
			}
			return super.selectUpdate(updateStream, releases.filter { it.tagName != "nightly" })
		}

		val releaseRegex = "commit: `(?<hash>[a-f0-9]+)`".toPattern()

		override fun findAsset(release: GithubRelease?): UpdateData? {
			val update = super.findAsset(release) as GithubReleaseUpdateData? ?: return null
			return GithubReleaseUpdateData(
				update.versionName,
				releaseRegex.matcher(update.releaseDescription)
					.takeIf { it.find() }
					?.run { group("hash") }
					?.let(::JsonPrimitive)
					?: update.versionNumber,
				update.sha256,
				update.download,
				update.releaseDescription,
				update.targetCommittish,
				update.createdAt,
				update.publishedAt,
				update.htmlUrl
			)
		}
	}

	init {
		UpdateUtils.patchConnection {
			this.requestUtil.enhanceConnection(it)
		}
	}

	var latestUpdate: PotentialUpdate? = null
	var hasNotified = false

	@SubscribeEvent
	fun onStartup(event: RegistrationFinishedEvent) {
		if (config.main.updateCheck == MainOptions.UpdateCheckBehaviour.NONE) return
		launchUpdateCheck()
	}

	fun launchUpdateCheck() {
		errorUtil.listenToFuture(
			updater.checkUpdate("nightly")
				.thenAcceptAsync(
					{
						latestUpdate = it
						informAboutUpdates(it)
					}, minecraftExecutor)
		)
	}

	@Inject
	lateinit var config: LedgerConfig

	@Inject
	lateinit var triggerCommand: TriggerCommand

	val installTrigger = "execute-download"

	@Inject
	lateinit var logger: LedgerLogger
	fun informAboutUpdates(potentialUpdate: PotentialUpdate) {
		if (hasNotified) return
		hasNotified = true
//		logger.printOut("Update: ${potentialUpdate}")
		if (!potentialUpdate.isUpdateAvailable) return
		logger.printOut(
			ChatComponentText("§aThere is a new update for LocalTransactionLedger. Click here to automatically download and install it.")
				.setChatStyle(ChatStyle().setChatClickEvent(triggerCommand.getTriggerCommandLine(installTrigger))))
		if (config.main.updateCheck == MainOptions.UpdateCheckBehaviour.FULL) {
			downloadUpdate()
		}
	}

	var updateFuture: CompletableFuture<Void>? = null

	fun downloadUpdate() {
		val l = latestUpdate ?: return
		if (updateFuture != null) return
		// TODO: inject into findAsset to overwrite the tag id with the commit id
		logger.printOut("§aTrying to download ledger update ${l.update.versionName}")
		updateFuture =
			latestUpdate?.launchUpdate()
				?.thenAcceptAsync(
					{
						logger.printOut("§aLedger update downloaded. It will automatically apply after your next restart.")
					}, minecraftExecutor)
				?.let(errorUtil::listenToFuture)
	}

	@SubscribeEvent
	fun onTrigger(event: TriggerEvent) {
		if (event.action == installTrigger) {
			event.isCanceled = true
			downloadUpdate()
		}
	}

}