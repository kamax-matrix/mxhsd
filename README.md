# mxhsd
[![Build Status](https://travis-ci.org/kamax-io/mxhsd.svg?branch=master)](https://travis-ci.org/kamax-io/mxhsd)  

----

## CURRENT STATUS
Given the inability to implement and document a functional Matrix protocol by New Vector and resolve security vulnerabilities  that are public for years, mxhsd will no longer try to implement the Matrix protocol. Instead it will implement what we (Kamax) think makes sense and document as we go.

This will be a gradual change while we reverse engineer the remaining of the protocol and improve it as we go. We might also support the Matrix protocol with a configuration option but on a best effort basis, if ever.

Thank you everyone for your interest until now. We hope to see you again as users of the new mxhsd (new name to come)

----

[Purpose](#purpose) | [Goals](#goals) | [Features](#features) | [Support](#support)

## Purpose
mxhsd is Matrix Homeserver aimed towards entities who want to have in-depth control of their servers, users and can truly integrate Matrix with their environment.

mxhsd is licensed under [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html)

## Goals
mxhsd wants to provide a Matrix Homeserver implementation with the expected level of admin, control and auditing corporations would expect, where every action performed by a user can be controlled by an ACL.

mxhsd wants to be as extensible as possible, relying on a module framework, so anyone can manage every detail of its server(s).

## Features
Version 0.1.0 aims to include the following features/abilities implemented to their strict minimum:

- Architecture
  - [ ] Permanent storage

- Client
  - Session
    - [X] Login
    - [X] Logout
  - Rooms
    - Management
      - [X] Create
    - Membership
      - [X] Join
      - [X] Leave
    - Usage
      - [X] Send events
    - Alias
      - [X] List
      - [X] Add
      - [X] Remove
      - [X] Lookup
  - Events
    - [X] Sync
    
- Server
  - Federation
    - Keys
      - Lookup
        - [X] Inbound
    - Protocol
      - Sign
        - [X] Requests
        - [X] Events
    - Rooms
      - Membership
        - Join
          - [ ] Inbound
          - [X] Outbound
        - [ ] Leave
      - Usage
        - [ ] Send events
      - Alias
        - [X] Lookup
    - Events
      - [ ] Inbound
      - [ ] Outbound
   - Admin
    - [ ] Administration page(s)

## Support
Via Matrix: [#mxhsd:kamax.io](https://matrix.to/#/#mxhsd:kamax.io) - [Static view](https://view.matrix.org/room/!MDGUnxWASkbvkdZMpE:kamax.io/)
