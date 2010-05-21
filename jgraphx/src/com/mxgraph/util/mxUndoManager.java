/**
 * $Id: mxUndoManager.java,v 1.12 2009/12/01 14:15:59 gaudenz Exp $
 * Copyright (c) 2007, Gaudenz Alder
 */
package com.mxgraph.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.mxgraph.model.mxGraphModel.mxChildChange;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;

/**
 * Implements an undo history.
 * 
 * This class fires the following events:
 * 
 * mxEvent.CLEAR fires after clear was executed. The event has no properties.
 * 
 * mxEvent.UNDO fires afer a significant edit was undone in undo. The
 * <code>edit</code> property contains the mxUndoableEdit that was undone.
 * 
 * mxEvent.REDO fires afer a significant edit was redone in redo. The
 * <code>edit</code> property contains the mxUndoableEdit that was redone.
 * 
 * mxEvent.ADD fires after an undoable edit was added to the history. The
 * <code>edit</code> property contains the mxUndoableEdit that was added.
 */
public class mxUndoManager extends mxEventSource
{
	
	int unmodifiedPosition;

	/**
	 * Maximum command history size. 0 means unlimited history. Default is 100.
	 */
	protected int size;

	/**
	 * List that contains the steps of the command history.
	 */
	protected List<mxUndoableEdit> history;

	/**
	 * Index of the element to be added next.
	 */
	protected int indexOfNextAdd;

	private boolean enabled=true;

	/**
	 * Constructs a new undo manager with a default history size.
	 */
	public mxUndoManager()
	{
		this(100);
	}

	/**
	 * Constructs a new undo manager for the specified size.
	 */
	public mxUndoManager(int size)
	{
		this.size = size;
		clear();
		resetUnmodifiedState();
	}

	/**
	 * 
	 */
	public boolean isEmpty()
	{
		return history.isEmpty();
	}

	/**
	 * Clears the command history.
	 */
	public void clear()
	{
		history = new ArrayList<mxUndoableEdit>(size);
		indexOfNextAdd = 0;
		fireEvent(new mxEventObject(mxEvent.CLEAR));
	}

	/**
	 * Returns true if an undo is possible.
	 */
	public boolean canUndo()
	{
		return indexOfNextAdd > 0;
	}

	/**
	 * Undoes the last change.
	 */
	public Collection<Object> undo()
	{
		HashSet<Object> modifiedObjects=new HashSet<Object>();
		while (indexOfNextAdd > 0)
		{
			mxUndoableEdit edit = history.get(--indexOfNextAdd);
			edit.undo();
			for (mxUndoableChange c:edit.getChanges()) {
				if (c instanceof mxChildChange) {
					Object o = ((mxChildChange) c).getChild();
					if (o!=null) modifiedObjects.add(o);
				}
			}

			if (edit.isSignificant())
			{
				fireEvent(new mxEventObject(mxEvent.UNDO, "edit", edit));
				break;
			}
		}
		return modifiedObjects;
	}

	/**
	 * Returns true if a redo is possible.
	 */
	public boolean canRedo()
	{
		return indexOfNextAdd < history.size();
	}

	/**
	 * Redoes the last change.
	 */
	public Collection<Object> redo()
	{
		HashSet<Object> modifiedObjects=new HashSet<Object>();
		int n = history.size();

		while (indexOfNextAdd < n)
		{
			mxUndoableEdit edit = history.get(indexOfNextAdd++);
			edit.redo();
			for (mxUndoableChange c:edit.getChanges()) {
				if (c instanceof mxChildChange) {
					Object o = ((mxChildChange) c).getChild();
					if (o!=null) modifiedObjects.add(o);
				}
			}
			
			if (edit.isSignificant())
			{
				fireEvent(new mxEventObject(mxEvent.REDO, "edit", edit));
				break;
			}
		}
		return modifiedObjects;
	}

	public void setEnabled(boolean e) {
		enabled=e;
	}
	
	/**
	 * Method to be called to add new undoable edits to the history.
	 */
	public void undoableEditHappened(mxUndoableEdit undoableEdit)
	{
		if (enabled) {
			if (undoableEdit.getTransparent()) {}
			else if (!undoableEdit.getUndoable()) {
				notUndoableEditHappened();
			} else {
				trim();
		
				if (size > 0 && size == history.size())
				{
					history.remove(0);
					unmodifiedPosition--;
				}
		
				history.add(undoableEdit);
				indexOfNextAdd = history.size();
				fireEvent(new mxEventObject(mxEvent.ADD, "edit", undoableEdit));
			}
		}
	}
	private boolean notUndoableEdits=false;
	public void notUndoableEditHappened() {
		notUndoableEdits=true;
	}

	/**
	 * Removes all pending steps after indexOfNextAdd from the history,
	 * invoking die on each edit. This is called from undoableEditHappened.
	 */
	protected void trim()
	{
		while (history.size() > indexOfNextAdd)
		{
			mxUndoableEdit edit = (mxUndoableEdit) history
					.remove(indexOfNextAdd);
			edit.die();
		}
	}

	
	
	public void resetUnmodifiedState() {
		//System.out.println("reset= "+indexOfNextAdd+" "+unmodifiedPosition);
		unmodifiedPosition=indexOfNextAdd;
		notUndoableEdits=false;
	}
	public boolean isUnmodifiedState() {
		//System.out.println("check= "+indexOfNextAdd+" "+unmodifiedPosition);
		return (!notUndoableEdits && (indexOfNextAdd==unmodifiedPosition));
	}
}