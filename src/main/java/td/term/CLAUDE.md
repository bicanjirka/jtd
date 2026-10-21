# `td.term`

- Neutral ground: depends on nothing in `td`. Every player-facing name is a `Term` owned by a domain
  enum (`EffectKind`, `TowerTerm`, `TraitTerm`, `AbilityTerm`); keys are `family.constant` in lower case.
- Player text is `TermText`, never a `String` that names a term. Wording rule: a mention uses the
  term's title, never a verb or adjective made from it ("applies Unravel", "immune to Freeze").
  `TermWordingTest` enforces it.
- Texts are lower-case first with no full stop; the UI finishes them with `asSentence()`. A number in
  a text comes from the constant or rule that holds it, written with `TermNumbers`.
