# mxhsd
[![Build Status](https://travis-ci.org/kamax-io/mxhsd.svg?branch=master)](https://travis-ci.org/kamax-io/mxhsd)  

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
      - [ ] Forget
    - Usage
      - [X] Send events
    - Alias
      - [ ] List
      - [ ] Add
      - [ ] Remove
      - [ ] Lookup
  - Events
    - [X] Sync
    
- Server
  - Federation
    - Keys
      - Lookup
        - [ ] Inbound
        - [ ] Outbound
    - Protocol
      - Sign
        - [ ] Requests
        - [ ] Events
    - Rooms
      - Membership
        - Join
          - [ ] Inbound
          - [ ] Outbound
        - [ ] Leave
      - Usage
        - [ ] Send events
      - Alias
        - [ ] Lookup
    - Events
      - [ ] Inbound
      - [ ] Outbound
   - Admin
    - [ ] Administration page(s)
    - [ ] Manage authentication backends - LDAP only

## Support
Via Matrix: [#mxhsd:kamax.io](https://matrix.to/#/#mxhsd:kamax.io) - [Static view](https://view.matrix.org/room/!MDGUnxWASkbvkdZMpE:kamax.io/)
