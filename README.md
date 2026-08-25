# VoidRP Client Fixes

A **client-only** NeoForge 1.21.1 mod that houses crash guards and compatibility
patches for buggy client-side behaviour in the VoidRP (FTB Evolution) modpack.

It is **not required on the server**: its dependencies are `side="CLIENT"` and it
declares `displayTest="IGNORE_ALL"`, so a client carrying it can join a server that
does not have it installed without any mod-list mismatch.

## Fixes

### `TipsyChatCrashGuardMixin` — Brewin' And Chewin' Tipsy chat crash

Brewin' And Chewin''s Tipsy chat handler (`TipsyEffects.iCanHear`) calls
`getChatMessage(msg)` unconditionally for every received chat message, and
`getChatMessage` assumes the message is a vanilla `chat.type.text` translatable
component with two args `[sender, content]`, reading `getArgs()[1]`. When the server
formats chat into a component that is not a 2-arg translatable (e.g. a plugin-formatted
chat message), `args[1]` is out of bounds:

```
java.lang.ArrayIndexOutOfBoundsException: Index 1 out of bounds for length 1
  at umpaz.brewinandchewin.neoforge.client.TipsyEffects.getChatMessage(TipsyEffects.java:50)
```

Since this runs on every client for every chat message, it disconnects **everyone**
whenever anyone talks. The mixin guards `getChatMessage`: if the message is not a
2-arg translatable it is returned unchanged (no garble, no crash). Well-formed vanilla
chat is untouched, so the Tipsy effect keeps working.

## Build

```bash
./gradlew build
# → build/libs/voidrp_client_fixes-1.0.0.jar
```

Install into the **client** modpack only.
