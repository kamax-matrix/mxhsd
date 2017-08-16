# mxhsd
[![Build Status](https://travis-ci.org/kamax-io/mxhsd.svg?branch=master)](https://travis-ci.org/kamax-io/mxhsd)  

[Purpose](#purpose) | [Origins](#origins) | [Goals](#goals) | [Features](#features) | [Support](#support)

## Purpose
mxhsd is Matrix Homeserver aimed towards entities who want to have in-depth control of their servers, users and can truly integrate Matrix with their environment.

mxhsd is licensed under [AGPLv3](https://www.gnu.org/licenses/agpl-3.0.en.html)

## Origins
Due to increasing frustration around the original Matrix project, the client and server implementations, the lack of protocol spec updates and a general lack of care for corporate environments, we decide to create a homeserver implementation (and related tools) that would empower the server admins just as much as the users.

## Goals
mxhsd wants to provide a Matrix Homeserver implementation with the expected level of admin, control and auditing corporations would expect, where every action performed by a user can be controlled by an ACL.

mxhsd wants to be as extensible as possible, relying on a module framework, so anyone can manage every detail of its server(s).

## Features
Version 0.1 aims to include the following features/abilities:

- Software side
  - [ ] Event-based module framework

- User side
  - [ ] Login/Logout
  - [ ] Set display name
  - [ ] Create, Join, Leave, Forget rooms
  - [ ] Send events in rooms - Only `m.room.message` will be supported

- Admin side
  - [ ] Administration page(s)
  - [ ] Purge (old events), delete rooms
  - [ ] Manage authentication backends - LDAP only

## Support
Via Matrix: [#mxhsd:kamax.io](https://matrix.to/#/#mxhsd:kamax.io) - [Static view](https://view.matrix.org/room/!MDGUnxWASkbvkdZMpE:kamax.io/)
