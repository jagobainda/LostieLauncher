# R8 rules for the release build.
#
# Empty on purpose. The libraries this side uses (Hilt, Compose,
# kotlinx.serialization, OkHttp) ship their own consumer rules, so a rule only
# belongs here once a release build is observed to need it. Adding speculative
# `-keep` rules is how a shrinker stops shrinking.
