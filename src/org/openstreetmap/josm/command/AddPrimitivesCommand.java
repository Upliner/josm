// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public class AddPrimitivesCommand extends Command {

    private final List<PrimitiveData> data = new ArrayList<PrimitiveData>();

    public AddPrimitivesCommand(List<PrimitiveData> data) {
        this.data.addAll(data);
    }
    
    public AddPrimitivesCommand(List<PrimitiveData> data, OsmDataLayer layer) {
        super(layer);
        this.data.addAll(data);
    }

    @SuppressWarnings("null")
    @Override public boolean executeCommand() {

        List<OsmPrimitive> createdPrimitives = new ArrayList<OsmPrimitive>(data.size());

        for (PrimitiveData pd:data) {
            OsmPrimitive primitive = getLayer().data.getPrimitiveById(pd);
            boolean created = primitive == null;
            if (created) {
                primitive = pd.getType().newInstance(pd.getUniqueId(), true);
            }
            if (pd instanceof NodeData) { // Load nodes immediately because they can't be added to dataset without coordinates
                primitive.load(pd);
            }
            if (created) {
                getLayer().data.addPrimitive(primitive);
            }
            createdPrimitives.add(primitive);
        }

        //Then load ways and relations
        for (int i=0; i<createdPrimitives.size(); i++) {
            if (!(createdPrimitives.get(i) instanceof Node)) {
                createdPrimitives.get(i).load(data.get(i));
            }
        }

        for (Iterator<OsmPrimitive> it = createdPrimitives.iterator();it.hasNext();) {
            OsmPrimitive p = it.next();
            if (p.getType() == OsmPrimitiveType.NODE && !p.getReferrers().isEmpty()) {
                it.remove();
            }
        }
        getLayer().data.setSelected(createdPrimitives);
        return true;
    }

    @Override public void undoCommand() {
        for (PrimitiveData p:data) {
            getLayer().data.removePrimitive(p);
        }
    }

    @Override public JLabel getDescription() {
        return new JLabel(trn("Added {0} object", "Added {0} objects", data.size(), data.size()), null,
                JLabel.HORIZONTAL
        );
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        // Does nothing because we don't want to create OsmPrimitives.
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        Collection<OsmPrimitive> prims = new HashSet<OsmPrimitive>();
        for (PrimitiveData d : data) {
            OsmPrimitive osm = getLayer().data.getPrimitiveById(d);
            if (osm == null)
                throw new RuntimeException();
            prims.add(osm);
        }
        return prims;
    }
}
