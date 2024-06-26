﻿# Lamport's Mutual Exclusion in Java
This project implements Lamport's mutual exclusion algorithm using logical clocks in a distributed system. The implementation involves communication between three devices via sockets, with access to a critical section assumed to be a file on any one of the three machines. The detailed explanation for the code can be found in the report file.

## Requirements
- `JDK/JDE`
- Code Editor
- Cloned repo

## Instruction
1. **Run `CriticalSection.java` First**: This file sets up the code that will have access to the critical section.

2. **Running the Processes on Multiple devices**
    - If you are running the processes on the same PC, no changes are needed.
    - Simply run `Main.java` on three different terminals for testing.
    - Or else edit the `add` variables with needed IP addresses in the `Main.java` file and run it on multiple devices.

3. **Input**
   - Input the current port no first. (Critical section runs on 8080, it can be changed by editing the `port_cs` variable)
   - Then input the other Instance's port no.
   - Interact with (1,2,3,4) digits with the menu as pleased.
  
## Authors
- `Akhil Kumar M (20CS01046)`
- `Sidhartha Sai M (20CS01006)`
- `Vinita Katlamudi (20CS01024)`
