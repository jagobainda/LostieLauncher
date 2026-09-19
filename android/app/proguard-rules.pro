# R8 rules for the release build.
#
# Empty on purpose. The libraries this side uses (Hilt, Compose,
# kotlinx.serialization, OkHttp) ship their own consumer rules, so a rule only
# belongs here once a release build is observed to need it. Adding speculative
# `-keep` rules is how a shrinker stops shrinking.
#
# Still empty after port plan step 04, but do not read that as proof that the
# network layer survives R8. Nothing in the UI reaches `ContentService` yet, so
# R8 strips the whole of `service/`, `model/` and Retrofit out of the release
# build and never exercises the reflective serializer lookup. The first step
# that binds a ViewModel to the content service has to run `assembleRelease`
# again and check that the generated `$serializer` classes are still in
# `build/outputs/mapping/release/mapping.txt`.
