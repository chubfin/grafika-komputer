import java.util.ArrayList;
import java.util.List;

public class ModifyShapesCommand implements Command {
    private final List<ShapeObject> shapes;
    private final List<ShapeState> beforeStates;
    private final List<ShapeState> afterStates;

    public ModifyShapesCommand(List<ShapeObject> shapes,
                               List<ShapeState> beforeStates,
                               List<ShapeState> afterStates) {
        this.shapes = new ArrayList<>(shapes);
        this.beforeStates = new ArrayList<>(beforeStates);
        this.afterStates = new ArrayList<>(afterStates);
    }

    @Override
    public void execute() {
        apply(afterStates);
    }

    @Override
    public void undo() {
        apply(beforeStates);
    }

    private void apply(List<ShapeState> states) {
        for (int i = 0; i < shapes.size() && i < states.size(); i++) {
            states.get(i).applyTo(shapes.get(i));
        }
    }
}
