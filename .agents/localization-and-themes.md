# Shared localization and theme rules

Read the platform localization or theme document for its implementation.

- User-visible text belongs in the language catalogue, including errors,
  tooltips and accessibility labels. Product names, brand names and language
  endonyms are deliberate exceptions.
- A new key needs a meaningful translation in all eight supported languages:
  Spanish, English, Catalan, Basque, Galician, Portuguese, Valencian and French.
- Preserve placeholder indices and their meaning in every translation.
- Language changes must update the current UI. Do not retain a resolved string
  where a later language change would leave it stale.
- Use theme roles rather than inline colors. Keep all ten themes complete:
  Volcarona, Zoroark, Infernape, Torterra, Empoleon, Mewtwo, Cefireon, Sylveon,
  Astrem and Auretoskos.
- Preserve the established palettes. An intentional design change needs its
  own scope and an explanation of its effect on both applications.
- Language and theme choices remain switchable at runtime and persisted.
