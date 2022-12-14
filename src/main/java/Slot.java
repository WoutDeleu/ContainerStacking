import java.util.List;
import java.util.Stack;

public class Slot {
    private int id;
    private int x;
    private int y;
    Stack<Integer> containerStack = new Stack<>();

    public Slot(Slot s) {
        this.id = s.getId();
        this.x = s.getX();
        this.y = s.getY();
    }

    public void addToContainerStack(int id) {
        containerStack.push(id);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getId() {
        return id;
    }
    public void popTopStack() {
        containerStack.pop();
    }


    public boolean isStackEmpty() {
        return !containerStack.isEmpty();
    }
    public int getHeightContainer(int containerId) {
        return containerStack.indexOf(containerId)+1;
    }
    public int getTotalHeight() {
        return containerStack.size();
    }
    public Stack<Integer> getContainerStack() {
        return containerStack;
    }

    public String printStackInfo() {
        if(containerStack.empty()) {
            return "(.. , ..)";
        }
        return "(" + containerStack.peek() + ", " + containerStack.size() + ")";
    }
    public String printStackContent() {
        StringBuilder sb = new StringBuilder();
        if(containerStack.empty()) {
            return "(...)";
        }
        sb.append("( ");
        for(Integer i : containerStack){
            sb.append(i.toString());
            sb.append(" , ");
        }
        sb.append(" )");
        return sb.toString();
    }

    public int peekStack() {
        return containerStack.peek();

    }

    public boolean containsContainer(int containerId) {
        return containerStack.contains(containerId);
    }
    // added in order of length
    public void addContainersExceedingHeight(int targetHeight, List<Integer> containersToMove) {
        for(int containerId : containerStack) {
            if(getHeightContainer(containerId) > targetHeight && !containersToMove.contains(containerId)) {
                int length = Main.containers.get(containerId).getLength();
                int index = 0;
                for(int id : containersToMove) {
                    if(Main.containers.get(id).getLength() >= length) break;
                    else index++;
                }

                containersToMove.add(index, containerId);
            }
        }
    }
}
