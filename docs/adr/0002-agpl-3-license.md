# 0002. AGPL-3.0 license

- **Status:** Accepted
- **Date:** 2026-11-19

## Context
The code is public for trust ("read the code that touches your bank data") and to build in public. A permissive license (MIT) would let anyone sell a closed copy. Source-available licenses (FSL, BSL) can ban competing use outright but aren't open source. Rewriting software has become cheap with AI, so a license can't stop someone from building a similar product; it can stop them from taking this code closed.

## Decision
License the code under AGPL-3.0. Require a contributor license agreement (CLA) for outside contributions. Protect the "Ranked Finance" name separately as a trademark.

## Consequences
- Anyone running a modified version as a service must publish their source, which deters closed forks.
- The maintainer, as sole copyright holder, can still relicense or sell the project, as long as contributions come in under the CLA.
- Dependencies must be AGPL-compatible (MIT, Apache-2.0, BSD, LGPL).
- The repo is public, so secrets must never be committed (secret scanning and push protection on).
