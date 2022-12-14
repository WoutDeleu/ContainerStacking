import java.util.*;
import java.util.List;

public class Main {
    private static Field field;
    private static List<Crane> cranes;
    private static final int SAFE_DISTANCE = 1;
    public static Map<Integer,Container> containers = new HashMap<>();

    public static void main(String[] args) throws Exception {
        int choice = chooseInputFile();
//        int choice = 4;
        System.out.println("inputFile: " + inputFiles[choice]);
        InputData inputData = InputData.readFile("data/" + inputFiles[choice] + ".json");
        inputData.formatAssignment();

        cranes = inputData.getCranes();
        containers = inputData.getContainersMap();
        field = new Field(inputData.getSlots(), inputData.getAssignmentsMap(), inputData.getMaxHeight());

        inputData.makeStacks(field);

        List<ContainerMovement> containerMovements;
        if(inputData.getTargetHeight() == 0) {
            String targetFile = targetFiles[choice];
            System.out.println("Reformat to target field " + targetFile);
            InputTarget inputTarget = InputTarget.readFile("data/" + targetFile + ".json");
            inputTarget.formatAssignment(inputData.getContainers(), inputData.getSlots());

            Field targetField = new Field(inputData.getSlots(), inputTarget.getAssignmentsMap(), inputTarget.getMaxheight());
            inputTarget.makeStacks(targetField, inputData.getContainers());

            List<Difference> differences = findDifferences(targetField);
            containerMovements = generateContainerMovements(differences);
            assert findDifferences(targetField).isEmpty(): "There are still differences between targetfield and own field";
        }
        else {
            // Format the container stack, so they don't exceed the target height
            System.out.println("Reformat so field doesn't exceed the target height");
            containerMovements = generateContainerMovements(inputData.getTargetHeight());
        }
        List<FullMovement> schedule = addCranesToMovement(containerMovements);
        printOfficialResult(schedule);
    }

    /**************************************************CRANES**************************************************/
    private static List<FullMovement> addCranesToMovement(List<ContainerMovement> containerMovements_List) throws Exception {
        Stack<ContainerMovement> containerMovements = new Stack();
        Collections.reverse(containerMovements_List);
        containerMovements.addAll(containerMovements_List);

        List<FullMovement> schedule = new ArrayList<>();
        // Contains times when cranes have finished their tasks
        Map<Integer, Double> craneTimeLocks = new HashMap<>(); // key = craneId, value = timestamp
        double timer = 0;
        while(!containerMovements.isEmpty()) {
            ContainerMovement containerMovement = containerMovements.pop();
            Coordinate containerStartLocation = containerMovement.getStart();
            Coordinate containerDestination = containerMovement.getEnd();

            // Select best Crane
            Crane crane = findBestCrane(containerMovement);
            if(crane.isInUse()) timer = updateTimeStamp(craneTimeLocks.get(crane.getId())+1, craneTimeLocks);

            CraneMovement moveToContainer = new CraneMovement(crane, containerStartLocation, timer);
            CraneMovement movingContainer = new CraneMovement(crane, containerStartLocation, containerDestination, timer + moveToContainer.travelTime()+1);

            // Check if crane can move the container all alone
            if(crane.inRange(containerMovement.getEnd())) {

                // Assumption: when a container must be moved to a position which needs the previous container being placed,
                // when this case takes place, it will be solved/fixed during the solveCollisions
                timer = solveCollisions(schedule, moveToContainer, movingContainer, craneTimeLocks, timer);

                // Add the movement itself to the schedule
                addToSchedule_withContainer(containerMovement, movingContainer, schedule, craneTimeLocks);
                System.out.println("Crane " + movingContainer.getCrane().getId() + " succesfully moved Container " + containerMovement.getContainerId());
            }
            else {
                Crane crane1 = crane;
                // Pass container to another crane, calculate meetup point
                Coordinate intermediateLocation = calculateMeetingPoint(crane1, containerMovement);

                /**Movements of crane 1
                    Moving container from startingpoint to meeting point*/
                CraneMovement moveToContainer_cr1 = new CraneMovement(crane1, containerStartLocation, timer);
                CraneMovement movingContainer_cr1 = new CraneMovement(crane1, containerStartLocation, intermediateLocation, timer + moveToContainer_cr1.travelTime()+1);

                solveCollisions(schedule, moveToContainer_cr1, movingContainer_cr1, craneTimeLocks, timer);

                addToSchedule_withContainer(containerMovement, movingContainer_cr1, schedule, craneTimeLocks);
                System.out.println("Crane " + movingContainer.getCrane().getId() + " succesfully moved Container " + containerMovement.getContainerId() +  " to intermediateLocation Location");


                /**Movements of crane 2
                    Moving container from meetingPoint to final destination*/
                ContainerMovement toFinalDestination = new ContainerMovement(containerMovement.getContainerId(), intermediateLocation, containerDestination);
                containerMovements.push(toFinalDestination);
            }
        }
        return schedule;
    }

    private static Coordinate calculateMeetingPoint(Crane crane, ContainerMovement containerMovement) {
        double xMax = crane.getXmax();
        double xMin = crane.getXmin();
        return field.calculatetMeetingPoint(xMax, xMin, containerMovement, containers.get(containerMovement.getContainerId()));
    }

    // solve collisions for full container & crane movement
    private static double solveCollisions(List<FullMovement> schedule, CraneMovement moveToContainer, CraneMovement movingContainer, Map<Integer, Double> craneTimeLocks, double timer) throws Exception {
        List<Crane> collisionCranes = detectCollision(moveToContainer, movingContainer, craneTimeLocks);
        // Loop over cranes that give issues
        while(!collisionCranes.isEmpty()) {
            Crane dangerousCrane = collisionCranes.get(0);
            // Wait for crane to finish, and update the timer
            if(dangerousCrane.isInUse()) {
                int id = dangerousCrane.getId();
                timer = craneTimeLocks.get(id)+1;
                executeEvents(timer, craneTimeLocks);
                moveToContainer.updateTimer(timer);
                movingContainer.updateTimer(moveToContainer.getEndTime());

                if(moveToContainer.collidesPosition(SAFE_DISTANCE, dangerousCrane, craneTimeLocks) || movingContainer.collidesPosition(SAFE_DISTANCE, dangerousCrane, craneTimeLocks)) {
                    // solve collision when moving container out of the way
                    CraneMovement moveCraneOutWay = dangerousCrane.moveToSaveDistance(timer, moveToContainer, movingContainer);
                    timer = moveCraneOutOfTheWay(moveCraneOutWay, timer, craneTimeLocks, schedule);

                    moveToContainer.updateTimer(timer);
                    movingContainer.updateTimer(moveToContainer.getEndTime()+1);

                }

                collisionCranes = detectCollision(moveToContainer, movingContainer, craneTimeLocks);
                assert !collisionCranes.contains(dangerousCrane) : "The collision with the current crane should be solved.";

            }
            // Move crane out of the way
            else {
                // solve collision when moving container out of the way
                CraneMovement moveCraneOutWay = dangerousCrane.moveToSaveDistance(timer, moveToContainer, movingContainer);
                timer = moveCraneOutOfTheWay(moveCraneOutWay, timer, craneTimeLocks, schedule);
                moveToContainer.updateTimer(timer);
                movingContainer.updateTimer(moveToContainer.getEndTime()+1);

                collisionCranes = detectCollision(moveToContainer, movingContainer, craneTimeLocks);
            }
        }
        return timer;
    }

    private static double moveCraneOutOfTheWay(CraneMovement moveCraneOutWay, double timer, Map<Integer, Double> craneTimeLocks, List<FullMovement> schedule) throws Exception {
        if (moveCraneOutWay != null) {
            double previousTimer = timer;
            timer = solveCollisionsRecurs(schedule, moveCraneOutWay, craneTimeLocks, timer);

            if (previousTimer == timer) {
                addToSchedule_withoutContainer(moveCraneOutWay, schedule, craneTimeLocks);
                timer = moveCraneOutWay.getEndTime() + 1;
            }
        }
        else {
            // This would happen in a field where one crane cannot move out of the way, this would be an unlikely terminal setup
            // Assunption : there is no crane for which the crane1.xmax > crane2.xmax en crane1.xmin < crane2.xmin
            throw new Exception("Crane cannot be moved out of the way");
            //oplossing:  todo: pass container on
        }
        return timer;
    }

    // Solve collision for movement crane out of the way
    private static double solveCollisionsRecurs(List<FullMovement> schedule, CraneMovement moveCraneOutWay, Map<Integer, Double> craneTimeLocks, double timer) throws Exception {
        List<Crane> collisionCranes = detectCollision(moveCraneOutWay, craneTimeLocks);
        // Loop over cranes that give issues
        while(!collisionCranes.isEmpty()) {
            Crane dangerousCrane = collisionCranes.get(0);
            // Wait for crane to finish, and update the timer
            if(dangerousCrane.isInUse()) {
                int id = dangerousCrane.getId();
                timer = craneTimeLocks.get(id)+1;
                executeEvents(timer, craneTimeLocks);
                moveCraneOutWay.updateTimer(timer);

                if(moveCraneOutWay.collidesPosition(SAFE_DISTANCE, dangerousCrane, craneTimeLocks)) {
                    // solve collision when moving container out of the way
                    CraneMovement moveOtherCraneOutWay = dangerousCrane.moveToSaveDistance(timer, moveCraneOutWay);
                    timer = moveCraneOutOfTheWay(moveOtherCraneOutWay, timer, craneTimeLocks, schedule);

                    moveCraneOutWay.updateTimer(timer);
                }
                collisionCranes = detectCollision(moveCraneOutWay, craneTimeLocks);
                assert !collisionCranes.contains(dangerousCrane) : "The collision with the current crane should be solved.";
            }
            // Move crane out of the way
            else {
                // solve collision when moving container out of the way
                CraneMovement moveOtherCraneOutWay = dangerousCrane.moveToSaveDistance(timer, moveCraneOutWay);
                timer =  moveCraneOutOfTheWay(moveOtherCraneOutWay, timer, craneTimeLocks, schedule);
                moveCraneOutWay.updateTimer(timer);
                collisionCranes = detectCollision(moveOtherCraneOutWay, craneTimeLocks);
            }
        }
        return timer;
    }

    private static void addToSchedule_withoutContainer(CraneMovement craneMove, List<FullMovement> schedule, Map<Integer, Double> craneTimeLocks) {
        Crane crane = getCraneWithId(craneMove.getCraneId());
        crane.setInUse();
        crane.addToTrajectory(craneMove);
        craneTimeLocks.put(crane.getId(), craneMove.getEndTime());
        schedule.add(new FullMovement(craneMove));
        crane.updateLocation(craneMove.getEndPoint());
    }

    private static void addToSchedule_withContainer(ContainerMovement containerMovement, CraneMovement craneMove, List<FullMovement> schedule, Map<Integer, Double> craneTimeLocks) {
        Crane crane = getCraneWithId(craneMove.getCraneId());
        crane.setInUse();
        crane.addToTrajectory(craneMove);
        craneTimeLocks.put(crane.getId(), craneMove.getEndTime());
        schedule.add(new FullMovement(containerMovement, craneMove));
        crane.updateLocation(craneMove.getEndPoint());
    }

    private static double updateTimeStamp(double newTimer, Map<Integer, Double> craneTimeLocks) {
        executeEvents(newTimer, craneTimeLocks);
        return newTimer;
    }

    private static void executeEvents(double newTimer, Map<Integer, Double> craneTimeLocks) {
        ArrayList<Integer> toRemove = new ArrayList<>();
        for(double time : craneTimeLocks.values()) {
            if(time <= newTimer) {
                List<Integer> cranes = Util.getValueFromMap(craneTimeLocks, time);
                for(int craneId : cranes) {
                    if(!toRemove.contains(craneId)) {
                        System.out.println("Crane " + craneId + " is no longer in use.");
                        Crane crane = getCraneWithId(craneId);
                        crane.setNotInUse();
                        toRemove.add(craneId);
                    }
                }
            }
        }
        for(int i : toRemove) {
            craneTimeLocks.remove(i);
        }
    }

    private static Crane getCraneWithId(int craneId) {
        for(Crane crane : cranes) {
            if(crane.getId() == craneId) return crane;
        }
        assert false: "No crane found for id " + craneId + ".";
        return null;
    }

    // solve collisions for full container & crane movement
    private static List<Crane> detectCollision(CraneMovement moveCraneOutWay, Map<Integer, Double> craneTimeLocks) {
        List<Crane> collisions = new ArrayList<>();
        for(Crane otherCrane : cranes) {
            if (otherCrane.getId() != moveCraneOutWay.getCraneId()) {
                if (moveCraneOutWay.collidesTraject(SAFE_DISTANCE, otherCrane) || moveCraneOutWay.collidesPosition(SAFE_DISTANCE, otherCrane, craneTimeLocks)) {
                    collisions.add(otherCrane);
                    System.out.println("Problems with crane " + otherCrane.getId() + ".");
                }
            }
        }
        return collisions;
    }

    // Solve collision for movement crane out of the way
    private static List<Crane> detectCollision(CraneMovement moveToContainer, CraneMovement movingContainer, Map<Integer, Double> craneTimeLocks) {
        List<Crane> collisions = new ArrayList<>();
        for(Crane otherCrane : cranes) {
            if (otherCrane.getId() != movingContainer.getCraneId()) {
                if (moveToContainer.collidesTraject(SAFE_DISTANCE, otherCrane) || movingContainer.collidesTraject(SAFE_DISTANCE, otherCrane) || moveToContainer.collidesPosition(SAFE_DISTANCE, otherCrane, craneTimeLocks) || movingContainer.collidesPosition(SAFE_DISTANCE, otherCrane, craneTimeLocks)) {
                    collisions.add(otherCrane);
                    System.out.println("Problems with crane " + otherCrane.getId() + ".");
                }
            }
        }
        return collisions;
    }

    public static Crane findBestCrane(ContainerMovement containerMovement) {
        Crane selectedCrane;
        List<Crane> cranedidates = new ArrayList<>();
        boolean perfectFound = false;
        boolean fullRangeFound = false;
        boolean acceptableFound = false;
        for(Crane crane : cranes) {
            if(crane.inRange(containerMovement.getStart())) {
                if(!crane.isInUse()) {
                    if(crane.inRange(containerMovement.getEnd())) {
                        if(!perfectFound) {
                            cranedidates.clear();
                            fullRangeFound = true;
                            perfectFound = true;
                            acceptableFound = true;
                        }
                        cranedidates.add(crane);
                    }
                    else if(!fullRangeFound) {
                        acceptableFound = true;
                        cranedidates.add(crane);
                    }
                }
                else {
                    if(crane.inRange(containerMovement.getEnd())) {
                        if(!perfectFound) {
                            if(!fullRangeFound) {
                                cranedidates.clear();
                                fullRangeFound = true;
                                acceptableFound = true;
                            }
                            cranedidates.add(crane);
                        }
                    }
                    else if(!acceptableFound) {
                        cranedidates.add(crane);
                    }

                }
            }
        }
        if(cranedidates.size() > 1) {
            double travelTime = Double.POSITIVE_INFINITY;
            Coordinate start = containerMovement.getStart();
            selectedCrane = cranedidates.get(0);
            for(Crane crane : cranedidates) {
                Coordinate craneCoord = crane.getLocation();
                double currentTravelTime =  crane.travelTime(start);
                if(currentTravelTime < travelTime) {
                    selectedCrane = crane;
                    travelTime = currentTravelTime;
                }
            }
            System.out.println("Best crane for current movement : " + selectedCrane.getId());
            return selectedCrane;
        }
        else {
            System.out.println("Best crane for current movement : " + cranedidates.get(0).getId());
            return cranedidates.get(0);
        }
    }
    /**************************************************CRANES**************************************************/


    /**************************************************MAX HEIGHT**************************************************/
    public static List<ContainerMovement> generateContainerMovements(int targetHeight) {
        List<ContainerMovement> containerMoves = new ArrayList<>();
        field.setMAX_HEIGHT(targetHeight);
        int currentIndex = 0;

        List<Integer> containersToMove = field.findContainersExceedingHeight(targetHeight);
        Stack<Integer> executed = new Stack<>();

        while(!containersToMove.isEmpty()) {
            int containerId = containersToMove.get(currentIndex);
            Container container = containers.get(containerId);

            List<Integer>[] possibleDestinationSlots = generatePossibleDestinations(targetHeight, container, containersToMove, containerMoves);

            List<Integer> destinationSlots = field.findBestAvailableSlot(container, containersToMove, possibleDestinationSlots);

            moveContainer(container, destinationSlots, containerMoves);
            executed.push(currentIndex);
            currentIndex++;

            if(currentIndex >= containersToMove.size()) {
                cleanUpDifferences(containersToMove, executed);
                currentIndex = 0;
            }
        }
        return containerMoves;
    }
    private static List<Integer>[] generatePossibleDestinations(int targetHeight, Container container, List<Integer> containersToMove, List<ContainerMovement> containerMoves) {
        List<Integer>[] possibleDestinationSlots = new List[0];
        int currentTargetHeight = targetHeight -1;
        while (possibleDestinationSlots.length == 0) {
            possibleDestinationSlots = field.findAvailableSlots(container);
            if (possibleDestinationSlots.length == 0) {
                ContainerMovement containerMovingTemp = field.makeRoom(container, containersToMove);
                if (containerMovingTemp == null) {
                    containerMovingTemp = field.lowerContainers(currentTargetHeight, containersToMove);
                    if(containerMovingTemp == null) {
                        assert currentTargetHeight > 1: "Cant lower containers";
                        currentTargetHeight--;
                    }
                }
                containerMoves.add(containerMovingTemp);
            }
        }
        return possibleDestinationSlots;
    }
    /**************************************************MAX HEIGHT**************************************************/



    /**************************************************TARGET FIELD**************************************************/
    // Used when creating new field based on target field
    private static List<ContainerMovement> generateContainerMovements(List<Difference> differences) {
        int currentIndex = 0;
        // Contains the movements of the container - with coordinates of the center of the container
        List<ContainerMovement> containerMoves = new ArrayList<>();
        // Stack containing all the indexes which are changed, so they can be removed from the differences
        // Stack -> Reverse order -> Easier to remove
        Stack<Integer> executed = new Stack<>();
        while (!differences.isEmpty()) {
            Difference diff = differences.get(currentIndex);
            int containerId = diff.getContainerId();

            List<Integer> destinationSlotIds = diff.getSlotIds();
            Container container = containers.get(containerId);
            // Try and place container to correct destination

            if (field.isValidContainerDestination(container, destinationSlotIds) && field.isMovableContainer(container)) {
                if (field.containerHasCorrectHeight(destinationSlotIds, diff.getHeight(), containerId)) {
                    moveContainer(container, destinationSlotIds, containerMoves);
                    executed.push(currentIndex);
                }
            }
            currentIndex++;

            if (currentIndex >= differences.size()) {
                cleanUpDifferences(differences, executed);
                currentIndex = 0;
            }
        }
        System.out.println("All containers are succesfully moved");
        return containerMoves;
    }
    private static <T> void cleanUpDifferences(List<T> differences, Stack<Integer> executed) {
        while(!executed.isEmpty()) {
            // De tussenvariabele index is om de een of andere reden nodig... Het werkt niet in 1 lijn
            int index = executed.pop();
            differences.remove(index);
        }
    }
    private static void moveContainer(Container container, List<Integer> destinationSlotIds, List<ContainerMovement> containerMoves) {
        Coordinate start = field.getGrabbingPoint(container.getId());
        field.moveContainer(container, destinationSlotIds);
        Coordinate end = field.getGrabbingPoint(container.getId());
        containerMoves.add(new ContainerMovement(container.getId(), start, end));
    }
    /**************************************************TARGET FIELD*************************************************/


    /*************************************************FIND DIFFERENCES*************************************************/
    private static List<Difference> findDifferences(Field targetField) {
        ArrayList<Integer[]> differences = new ArrayList<>();
        for(Slot slot :  field.getSlots()) {
            Slot targetSlot = targetField.getSlot_slotId(slot.getId());
            if(slot.getTotalHeight() == 0) {
                // If a slot is empty for the original, but contains containers for the target
                // All the target slots need to be saved in differences
                if(targetSlot.getTotalHeight() != 0) {
                    for(int i = 0; i < targetSlot.getTotalHeight(); i++) {
                        int containerId = targetSlot.getContainerStack().get(i);
                        differences.add(new Integer[]{containerId,  targetSlot.getHeightContainer(containerId) ,targetSlot.getId()});
                    }
                }
            }
            else {
                // Compare 2 stacks, and extract the differences
                int minHeight = Math.min(slot.getTotalHeight(), targetSlot.getTotalHeight());
                for(int i = 0; i < minHeight; i++) {
                    Stack<Integer> original = slot.getContainerStack();
                    Stack<Integer> target = targetSlot.getContainerStack();
                    if(!original.get(i).equals(target.get(i))) {
                        int containerId = target.get(i);
                        differences.add(new Integer[]{containerId,  targetSlot.getHeightContainer(containerId), targetSlot.getId()});
                    }
                }
                if(targetSlot.getTotalHeight() > minHeight) {
                    for(int i = minHeight; i < targetSlot.getTotalHeight(); i++) {
                        int containerId = targetSlot.getContainerStack().get(i);
                        differences.add(new Integer[]{containerId,  targetSlot.getHeightContainer(containerId) ,targetSlot.getId()});
                    }
                }

            }
        }
        return convertDifferencesToAssignments(differences);
    }
    private static List<Difference> convertDifferencesToAssignments(ArrayList<Integer[]> differences_slotId_height_containerId) {
        List<Difference> differences = new ArrayList<>();
        for(Integer[] diff : differences_slotId_height_containerId) {
            int containerId = diff[0];
            int height = diff[1];
            int slotId = diff[2];

            // Check if assignment of this container is already filled in
            boolean containsContainer = false;
            for(Difference diff_alreayPresent : differences) {
                if(diff_alreayPresent.getContainerId() == containerId) containsContainer = true;
            }
            // Find the other slots on which this container must be placed
            if(!containsContainer) {
                List<Integer> slots = new ArrayList<>();
                slots.add(slotId);
                for(Integer[] values : differences_slotId_height_containerId) {
                    if(values[0] == containerId && !slots.contains(values[2])) {
                        slots.add(values[2]);
                    }
                }
                // Format to a Difference object
                slots.sort(Comparator.comparingInt(id -> id));
                differences.add(new Difference(height, new Assignment(containerId, slots)));
            }
        }
        return differences;
    }
    /*************************************************FIND DIFFERENCES*************************************************/

    private static void printOfficialResult(List<FullMovement> schedule) {
        System.out.println();
        System.out.println("\nFull schedule: ");
        System.out.println("%CraneId;ContainerId;PickupTime;EndTime;PickupPosX;PickupPosY;EndPosX;EndPosY;");
        for(FullMovement f : schedule) {
            System.out.println(f);
        }
    }
    private static void printDebugginResult(List<FullMovement> schedule) {
        System.out.println();
        System.out.println("\nFull schedule: ");
        System.out.println("%CraneId;ContainerId;PickupTime;EndTime;PickupPosX;PickupPosY;EndPosX;EndPosY;");
        for(FullMovement f : schedule) {
            System.out.println(f.testToString());
        }
    }
    public static void visualizeField() {
        System.out.println("x ->");
        System.out.println("y |");
        System.out.println();
        Slot[][] fieldMatrix = field.getFieldMatrix();
        for(int i = 0 ; i < fieldMatrix.length; i++) {
            for(int j = 0 ; j < fieldMatrix[i].length; j++) {
                if(fieldMatrix[i][j] != null) {
                    System.out.print(fieldMatrix[i][j].printStackInfo() + "\t");
                }
                else System.out.print("....\t" );
            }
            System.out.println();
        }
    }
    private static void testBasicFunctionality() {
        ArrayList<Integer> list = new ArrayList<>(1);
        Container container = new Container(7, 1);
        containers.put(7, container);
        field.placeContainer(new Container(6, 2), new ArrayList<>(Arrays.asList(1, 2)));
        field.placeContainer(new Container(5, 1), new ArrayList<>(Arrays.asList(3)));
        field.placeContainer(container, new ArrayList<>(Arrays.asList(3)));
        if(field.isValidContainerDestination(container, new ArrayList<>(Arrays.asList(2))) && field.isMovableContainer(container)) field.moveContainer(container, new ArrayList<Integer>(Arrays.asList(2)));

        for(Container container2 : containers.values()) {
            System.out.println(field.getGrabbingPoint(container.getId()));
        }
    }
    /*************************************************TESTING*************************************************/



    /*************************************************I/O*************************************************/
    private static String[] inputFiles = new String[]{"terminal22_1_100_1_10", "1t/TerminalA_20_10_3_2_100", "2mh/MH2Terminal_20_10_3_2_100","3t/TerminalA_20_10_3_2_160", "4mh/MH2Terminal_20_10_3_2_160", "5t/TerminalB_20_10_3_2_160", "5tUPDATE/TerminalB_20_10_3_2_160" , "6t/Terminal_10_10_3_1_100", "7t/TerminalC_10_10_3_2_80", "8t/TerminalC_10_10_3_2_80", "9t/TerminalC_10_10_3_2_100", "10t/TerminalC_10_10_3_2_100", "Terminal_20_10_3_2_100-HEIGHT"};
    private static String[] targetFiles = new String[]{"terminal22_1_100_1_10target", "1t/targetTerminalA_20_10_3_2_100", null, "3t/targetTerminalA_20_10_3_2_160", null, "5t/targetTerminalB_20_10_3_2_160" , "5tUPDATE/targetTerminalB_20_10_3_2_160UPDATE", "6t/targetTerminal_10_10_3_1_100", "7t/targetTerminalC_10_10_3_2_80", "8t/targetTerminalC_10_10_3_2_80", "9t/targetTerminalC_10_10_3_2_100", "10t/targetTerminalC_10_10_3_2_100", null};
    private static int chooseInputFile() {
        for(int i=0; i<inputFiles.length; i++) {
            System.out.print(i + ": ");
            System.out.println(inputFiles[i]);
        }
        System.out.println("Select the input file");
        Scanner sc = new Scanner(System.in);
        int choice = sc.nextInt();
        System.out.println();
        return choice;
    }
    /*************************************************I/O*************************************************/
}
