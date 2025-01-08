-keep class !moe.nea.ledger.gen.** {*;}
-dontobfuscate
-assumenosideeffects class ** { @moe.nea.ledger.utils.NoSideEffects <methods>; }
#-dontoptimize
