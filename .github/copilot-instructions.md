# Copilot instructions — Port Royal Back End

This file contains agent-specific constraints only. For project structure, build and test commands,
API contracts, database details, and client flows, consult the [README](../README.md), the
[Game Rules and API Guide](../docs/GAME_RULES_AND_API.md), the
[Client Development Guide](../docs/CLIENT_DEVELOPMENT_GUIDE.md), and the source code. Do not
duplicate details maintained in those documents here.

## Explicit authorization for Git history and remote changes

Copilot must never change Git history or remote repository state unless the user has explicitly
authorized the specific operation beforehand. If the requested operation or its scope is not
explicit, describe it, ask the user, and wait for explicit approval before acting. This includes
committing, amending, squashing, rebasing, merging, cherry-picking, reverting, resetting, creating
or deleting branches or tags, and pushing (including force-pushing). Never infer authorization
from routine work, testing, cleanup, or approval for a different operation. This restriction does
not prohibit working-tree edits explicitly requested by the user.

## Language

All comments and written documentation added or changed in the repository must be in English.

## Game-rule ownership

All game rules and move validation belong in `portroyal_lib`. Keep `portroyal_be` as an API and
orchestration layer: it forwards player input to the library, which validates and applies moves.
Never duplicate or implement game-rule logic in `portroyal_be`.

## Identity and BOT invariants

Preserve the frontend-agnostic identity model. The Telegram integration uses one technical `BOT`
account, not one account per Telegram player. For BOT-mediated requests, authenticate the BOT first
and resolve the acting player from the required `tgId`; do not treat `tgId` as authentication.
Matches and moves must remain associated with the domain user, not the client type.

## Persistence and instance state

PostgreSQL is the source of truth for accepted moves and shared application state. Treat local caches
only as disposable performance optimizations; validate or rebuild cached state against persistence
where required. Do not introduce instance-local state for functionality that must work across
instances, or replace existing database-backed queues with in-memory side effects.
