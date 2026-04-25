# klor-experiments

A CRUD web app for experimenting with [klor](https://github.com/lovrosdu/klor), a choreographic programming library for Clojure.

## What is klor?

Klor lets you write a *choreography* — a single piece of code that describes how multiple participants communicate — and automatically derives each participant's individual implementation (their *projection*).

Instead of writing separate client and server code and hoping they stay in sync, you write the protocol once and klor generates both sides.

## How it works in this app

The app has two roles: `Client` (the HTTP handler) and `Server` (the storage layer). Each CRUD operation is a choreography:

```clojure
(defchor create-user [Client Server]
  (-> #{Client} #{Client})
  [user-data]
  (let [sdata (Client->Server user-data)]
    (Server->Client (Server (storage/create! sdata)))))
```

Reading this choreography step by step:

- `[Client Server]` — two participants
- `(-> #{Client} #{Client})` — type signature: takes a value known to `Client`, returns a value known to `Client`
- `[user-data]` — one parameter, held by `Client` (matches the `#{Client}` in the type)
- `(Client->Server user-data)` — `Client` sends `user-data` to `Server`; result `sdata` is now held by `Server`
- `(Server (storage/create! sdata))` — `Server` runs `create!` locally; result is held by `Server`
- `(Server->Client ...)` — `Server` sends the result back to `Client`

Klor projects this into two independent implementations at compile time:

| Client projection | Server projection |
|---|---|
| send `user-data` to Server | receive `user-data` from Client |
| wait to receive result | run `storage/create!` |
| return result | send result to Client |

### `->` vs `=>`

Klor has two communication operators:

- `A->B` (*move*) — `A` sends to `B`; afterwards only `B` holds the value. Type narrows to `#{B}`.
- `A=>B` (*copy*) — `A` sends to `B`; both hold the value. Type widens to `#{A B}`.

This app uses `->` throughout since `Server` doesn't need to keep a reference to values it has already stored.

### Running choreographies

Each HTTP handler calls `run-chor`, which uses `simulate-chor` to run both role projections concurrently in the same JVM via `core.async` go-blocks:

```clojure
(defn run-chor [chor & args]
  (get @(apply simulate-chor chor args) 'Client))
```

`simulate-chor` returns a `delay` that resolves to a map of `{role -> return-value}` for each participant. The keys are symbols matching the role names in `defchor` (e.g. `'Client`, `'Server`). `run-chor` derefs the delay and extracts the `Client` role's return value, which is what the HTTP handler returns to the browser.

### The type system

Klor tracks *which roles know which values* through the type system. An agreement type `#{Client}` means "a value known only to `Client`". The type checker enforces that you never accidentally use a value at a role that doesn't hold it.

The type signature `(-> #{Client} #{Client})` on `create-user` means:
- it takes one argument of type `#{Client}` (only Client knows it)
- it returns a value of type `#{Client}` (only Client gets the result back)

If you tried to use `user-data` directly on the `Server` without first sending it, klor would refuse to compile.

## Project structure

```
src/klor_experiments/
  core.clj          Ring server + reitit routes + run-chor helper
  choreography.clj  defchor definitions for list, create, update, delete
  storage.clj       Atom-backed user store
  html.clj          Hiccup views
dev/
  user.clj          Dev REPL: auto-starts server with hot reload
test/klor_experiments/
  core_test.clj     Tests for storage, choreographies, and HTTP handlers
```

## Running

```bash
# Dev server (auto-opens browser, hot reloads on save)
clj -A:dev

# Tests
clj -M:test -e "(require 'klor-experiments.core-test) (clojure.test/run-tests 'klor-experiments.core-test)"
```

## Further reading

- [klor on GitHub](https://github.com/lovrosdu/klor) — source, docs, and examples
- `klor.simulator/simulate-chor` — in-memory simulation via core.async (used here)
- `klor.sockets` + `klor.runtime/play-role` — run projections in separate processes over TCP sockets (the next step for a real distributed setup)
