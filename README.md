# sharer
simple Peer2Peer file sharing tool

No setup of any server is required. Just start the application and start sharing files within your local network. As fast as possible! The more nodes the better the performance!

Happy sharing.

## Implemented
- share files via dropping them into the dropping area
- discovery of other sharing nodes
- shared files will be transmitted in chunks which are loaded from available nodes to use all available network capacity
- calculated metadata for shared files can be saved/loaded (\<user.home\>/.sharer)

## Known Issues
- GUI is not that fancy yet
- does work only on the same network segment - broadcast domain
- directories are not supported yet
- ...

## Dependencies
- gson-2.6.1 or higher - https://github.com/google/gson

**Work in progress...**
