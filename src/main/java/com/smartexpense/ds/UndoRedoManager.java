package com.smartexpense.ds;

import java.util.ArrayDeque;
import java.util.Deque;

/** Command-pattern history built on two stacks (undo + redo). */
public class UndoRedoManager {

    public interface Command {
        void execute() throws Exception;

        void undo() throws Exception;

        String label();
    }

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    private static final int LIMIT = 100;

    public void run(Command c) throws Exception {
        c.execute();
        undoStack.push(c);
        if (undoStack.size() > LIMIT) undoStack.removeLast();
        redoStack.clear();
    }

    /** @return the label of the undone command, or null when nothing to undo */
    public String undo() throws Exception {
        if (undoStack.isEmpty()) return null;
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        return c.label();
    }

    /** @return the label of the redone command, or null when nothing to redo */
    public String redo() throws Exception {
        if (redoStack.isEmpty()) return null;
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
        return c.label();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }
    public int undoDepth() { return undoStack.size(); }
    public int redoDepth() { return redoStack.size(); }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }
}
