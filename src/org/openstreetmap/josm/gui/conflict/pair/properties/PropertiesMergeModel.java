// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.properties;

import static org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType.UNDECIDED;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.CoordinateConflictResolveCommand;
import org.openstreetmap.josm.command.DeletedStateConflictResolveCommand;
import org.openstreetmap.josm.command.VisibleStateConflictResolveCommand;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the model for resolving conflicts in the properties of the
 * {@see OsmPrimitive}s. In particular, it represents conflicts in the coordiates of {@see Node}s and
 * the deleted or visible state of {@see OsmPrimitive}s.
 *
 * This model is an {@see Observable}. It notifies registered {@see Observer}s whenever the
 * internal state changes.
 *
 * This model also emits property changes for {@see #RESOLVED_COMPLETELY_PROP}. Property change
 * listeners may register themselves using {@see #addPropertyChangeListener(PropertyChangeListener)}.
 *
 * @see Node#getCoor()
 * @see OsmPrimitive#deleted
 * @see OsmPrimitive#visible
 *
 */
public class PropertiesMergeModel extends Observable {

    static public final String RESOLVED_COMPLETELY_PROP = PropertiesMergeModel.class.getName() + ".resolvedCompletely";
    static public final String DELETE_PRIMITIVE_PROP = PropertiesMergeModel.class.getName() + ".deletePrimitive";

    private OsmPrimitive my;
    private OsmPrimitive their;

    private LatLon myCoords;
    private LatLon theirCoords;
    private MergeDecisionType coordMergeDecision;

    private boolean myDeletedState;
    private boolean theirDeletedState;
    private boolean myVisibleState;
    private boolean theirVisibleState;
    private List<OsmPrimitive> myReferrers;
    private List<OsmPrimitive> theirReferrers;
    private MergeDecisionType deletedMergeDecision;
    private MergeDecisionType visibleMergeDecision;
    private final PropertyChangeSupport support;
    private boolean resolvedCompletely;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public void fireCompletelyResolved() {
        boolean oldValue = resolvedCompletely;
        resolvedCompletely = isResolvedCompletely();
        support.firePropertyChange(RESOLVED_COMPLETELY_PROP, oldValue, resolvedCompletely);
    }

    public PropertiesMergeModel() {
        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        support = new PropertyChangeSupport(this);
        resolvedCompletely = false;
    }

    /**
     * replies true if there is a coordinate conflict and if this conflict is
     * resolved
     *
     * @return true if there is a coordinate conflict and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedCoord() {
        return ! coordMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if there is a  conflict in the deleted state and if this conflict is
     * resolved
     *
     * @return true if there is a conflict in the deleted state and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedDeletedState() {
        return ! deletedMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if there is a  conflict in the visible state and if this conflict is
     * resolved
     *
     * @return true if there is a conflict in the visible state and if this conflict is
     * resolved; false, otherwise
     */
    public boolean isDecidedVisibleState() {
        return ! visibleMergeDecision.equals(UNDECIDED);
    }

    /**
     * replies true if the current decision for the coordinate conflict is <code>decision</code>
     *
     * @return true if the current decision for the coordinate conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isCoordMergeDecision(MergeDecisionType decision) {
        return coordMergeDecision.equals(decision);
    }

    /**
     * replies true if the current decision for the deleted state conflict is <code>decision</code>
     *
     * @return true if the current decision for the deleted state conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isDeletedStateDecision(MergeDecisionType decision) {
        return deletedMergeDecision.equals(decision);
    }

    /**
     * replies true if the current decision for the visible state conflict is <code>decision</code>
     *
     * @return true if the current decision for the visible state conflict is <code>decision</code>;
     *  false, otherwise
     */
    public boolean isVisibleStateDecision(MergeDecisionType decision) {
        return visibleMergeDecision.equals(decision);
    }
    /**
     * populates the model with the differences between my and their version
     *
     * @param my my version of the primitive
     * @param their their version of the primitive
     */
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        this.my = conflict.getMy();
        this.their = conflict.getTheir();
        if (my instanceof Node) {
            myCoords = ((Node)my).getCoor();
            theirCoords = ((Node)their).getCoor();
        } else {
            myCoords = null;
            theirCoords = null;
        }

        myDeletedState =  conflict.isMyDeleted() || my.isDeleted();
        theirDeletedState = their.isDeleted();

        myVisibleState = my.isVisible();
        theirVisibleState = their.isVisible();

        myReferrers = my.getDataSet() == null?Collections.<OsmPrimitive>emptyList():my.getReferrers();
        theirReferrers = their.getDataSet() == null?Collections.<OsmPrimitive>emptyList():their.getReferrers();

        coordMergeDecision = UNDECIDED;
        deletedMergeDecision = UNDECIDED;
        visibleMergeDecision = UNDECIDED;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * replies the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     *
     * @return the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     *  coordinates (i.e. because it is a {@see Way}).
     */
    public LatLon getMyCoords() {
        return myCoords;
    }

    /**
     * replies the coordinates of their {@see OsmPrimitive}. null, if their primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     *
     * @return the coordinates of my {@see OsmPrimitive}. null, if my primitive hasn't
     * coordinates (i.e. because it is a {@see Way}).
     */
    public LatLon getTheirCoords() {
        return theirCoords;
    }

    /**
     * replies the coordinates of the merged {@see OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@see MergeDecisionType#UNDECIDED}
     *
     * @return the coordinates of the merged {@see OsmPrimitive}. null, if the current primitives
     * have no coordinates or if the conflict is yet {@see MergeDecisionType#UNDECIDED}
     */
    public LatLon getMergedCoords() {
        switch(coordMergeDecision) {
        case KEEP_MINE: return myCoords;
        case KEEP_THEIR: return theirCoords;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    /**
     * decides a conflict between my and their coordinates
     *
     * @param decision the decision
     */
    public void decideCoordsConflict(MergeDecisionType decision) {
        coordMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * replies my deleted state,
     * @return
     */
    public Boolean getMyDeletedState() {
        return myDeletedState;
    }

    public  Boolean getTheirDeletedState() {
        return theirDeletedState;
    }

    public Boolean getMergedDeletedState() {
        switch(deletedMergeDecision) {
        case KEEP_MINE: return myDeletedState;
        case KEEP_THEIR: return theirDeletedState;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    /**
     * replies my visible state,
     * @return my visible state
     */
    public Boolean getMyVisibleState() {
        return myVisibleState;
    }

    /**
     * replies their visible state,
     * @return their visible state
     */
    public  Boolean getTheirVisibleState() {
        return theirVisibleState;
    }

    /**
     * returns my referrers,
     * @return my referrers
     */
    public List<OsmPrimitive> getMyReferrers() {
        return myReferrers;
    }

    /**
     * returns their referrers,
     * @return their referrers
     */
    public List<OsmPrimitive> getTheirReferrers() {
        return theirReferrers;
    }

    /**
     * replies the merged visible state; null, if the merge decision is
     * {@see MergeDecisionType#UNDECIDED}.
     *
     * @return the merged visible state
     */
    public Boolean getMergedVisibleState() {
        if (my.getVersion() != their.getVersion()) {
            if (my.getVersion() > their.getVersion())
                return my.isVisible();
            else
                return their.isVisible();
        }
        switch(visibleMergeDecision) {
        case KEEP_MINE: return myVisibleState;
        case KEEP_THEIR: return theirVisibleState;
        case UNDECIDED: return null;
        }
        // should not happen
        return null;
    }

    private boolean getMergedDeletedState(MergeDecisionType decision) {
        switch (decision) {
        case KEEP_MINE:
            return myDeletedState;
        case KEEP_THEIR:
            return theirDeletedState;
        default:
            return false;
        }
    }

    /**
     * decides the conflict between two deleted states
     * @param decision the decision (must not be null)
     *
     * @throws IllegalArgumentException thrown, if decision is null
     */
    public void decideDeletedStateConflict(MergeDecisionType decision) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(decision, "decision");

        boolean oldMergedDeletedState = getMergedDeletedState(this.deletedMergeDecision);
        boolean newMergedDeletedState = getMergedDeletedState(decision);

        this.deletedMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();

        if (oldMergedDeletedState != newMergedDeletedState) {
            support.firePropertyChange(DELETE_PRIMITIVE_PROP, oldMergedDeletedState, newMergedDeletedState);
        }
    }

    /**
     * decides the conflict between two visible states
     * @param decision the decision (must not be null)
     *
     * @throws IllegalArgumentException thrown, if decision is null
     */
    public void decideVisibleStateConflict(MergeDecisionType decision) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(decision, "decision");
        this.visibleMergeDecision = decision;
        setChanged();
        notifyObservers();
        fireCompletelyResolved();
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their coordinate values
     *
     * @return true if my and their primitive have a conflict between
     * their coordinate values; false otherwise
     */
    public boolean hasCoordConflict() {
        if (myCoords == null && theirCoords != null) return true;
        if (myCoords != null && theirCoords == null) return true;
        if (myCoords == null && theirCoords == null) return false;
        return !myCoords.equalsEpsilon(theirCoords);
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their deleted states
     *
     * @return true if my and their primitive have a conflict between
     * their deleted states
     */
    public boolean hasDeletedStateConflict() {
        return myDeletedState != theirDeletedState;
    }

    /**
     * replies true if my and their primitive have a conflict between
     * their visible states
     *
     * @return true if my and their primitive have a conflict between
     * their visible states
     */
    public boolean hasVisibleStateConflict() {
        return myVisibleState != theirVisibleState && my.getVersion() == their.getVersion();
    }

    /**
     * replies true if all conflict in this model are resolved
     *
     * @return true if all conflict in this model are resolved; false otherwise
     */
    public boolean isResolvedCompletely() {
        boolean ret = true;
        if (hasCoordConflict()) {
            ret = ret && ! coordMergeDecision.equals(UNDECIDED);
        }
        if (hasDeletedStateConflict()) {
            ret = ret && ! deletedMergeDecision.equals(UNDECIDED);
        }
        if (hasVisibleStateConflict()) {
            ret = ret && ! visibleMergeDecision.equals(UNDECIDED);
        }
        return ret;
    }

    /**
     * builds the command(s) to apply the conflict resolutions to my primitive
     *
     * @param my  my primitive
     * @param their their primitive
     * @return the list of commands
     */
    public List<Command> buildResolveCommand(Conflict<? extends OsmPrimitive> conflict) {
        List<Command> cmds = new ArrayList<Command>();
        if (hasVisibleStateConflict() && isDecidedVisibleState()) {
            cmds.add(new VisibleStateConflictResolveCommand(conflict, visibleMergeDecision));
        }
        if (hasCoordConflict() && isDecidedCoord()) {
            cmds.add(new CoordinateConflictResolveCommand(conflict, coordMergeDecision));
        }
        if (hasDeletedStateConflict() && isDecidedDeletedState()) {
            cmds.add(new DeletedStateConflictResolveCommand(conflict, deletedMergeDecision));
        }
        return cmds;
    }

    public OsmPrimitive getMyPrimitive() {
        return my;
    }

}
