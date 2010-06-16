// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Represents a the resolution of a conflict between two {@see OsmPrimitive}s one of which has wrong "visible" property
 *
 */
public class VisibleStateConflictResolveCommand extends ConflictResolveCommand {

    /** the conflict to resolve */
    private Conflict<? extends OsmPrimitive> conflict;

    /** the merge decision */
    private final MergeDecisionType decision;

    /**
     * constructor
     *
     * @param my  my node
     * @param their  their node
     * @param decision the merge decision
     */
    public VisibleStateConflictResolveCommand(Conflict<? extends OsmPrimitive> conflict, MergeDecisionType decision) {
        this.conflict = conflict;
        this.decision = decision;
    }

    @Override public JLabel getDescription() {
        return new JLabel(
                tr("Resolve conflicts in deleted state in {0}",conflict.getMy().getId()),
                ImageProvider.get("data", "object"),
                JLabel.HORIZONTAL
        );
    }

    @Override
    public boolean executeCommand() {
        // remember the current state of modified primitives, i.e. of
        // OSM primitive 'my'
        //
        super.executeCommand();

        if (decision.equals(MergeDecisionType.KEEP_MINE)) {
            // do nothing
        } else if (decision.equals(MergeDecisionType.KEEP_THEIR)) {
            OsmPrimitive my = conflict.getMy();
            boolean visible = conflict.getTheir().isVisible();
            my.setVisible(visible);
            if (!visible) {
                my.setModified(!my.isDeleted());
            }
        } else
            // should not happen
            throw new IllegalStateException(tr("Cannot resolve undecided conflict."));

        // remember the layer this command was applied to
        //
        rememberConflict(conflict);

        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        modified.add(conflict.getMy());
    }
}
