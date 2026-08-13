# ServerFuser
[EXPERIMENTAL] Trying to make multiple distributed NeoForge Minecraft servers act like 1 big Minecraft server.

# TO DO:
[] Connect 4 servers to Velocity
[] Share chat between 4 servers
[] Share the same seed to 4 servers and tell the servers what chunks they need to generate (Quadrant Offset System)
[] Transfer players when they reach server border
[] Add player data persistence between servers
[] Ghost Chunks (static) - Add a client side only way to view a mirage of the bordering server's chunks (at least a few of them) that updates every 1 minute.
[] Entity Transfer - When an entity goes to a server border, serialize its data, send it to the next server, spawn it there and destroy it on the sender server.
[] Ghost Chunks (live updates) - Make block changes, entities and players visible live in chunk mirages.
[] Create Compat
  [] Trains
    [] Make a way to share the train transportation graph.
    [] Make a way for trains to be able to go between servers.
    [] Decide what do to when a train is in the middle of a server border.
[] Map Compat
 [] Make map mods belive the server is 1 big server instead of multiple smaller ones.
