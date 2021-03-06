// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel.PresetHandler;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This dialog displays the properties of the current selected primitives.
 *
 * If no object is selected, the dialog list is empty.
 * If only one is selected, all properties of this object are selected.
 * If more than one object are selected, the sum of all properties are displayed. If the
 * different objects share the same property, the shared value is displayed. If they have
 * different values, all of them are put in a combo box and the string "&lt;different&gt;"
 * is displayed in italic.
 *
 * Below the list, the user can click on an add, modify and delete property button to
 * edit the table selection value.
 *
 * The command is applied to all selected entries.
 *
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener, MapView.EditLayerChangeListener, DataSetListenerAdapter.Listener {
    /**
     * Watches for double clicks and from editing or new property, depending on the
     * location, the click was.
     * @author imi
     */
    public class DblClickWatch extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2)
            {
                if (e.getSource() == propertyTable) {
                    membershipTable.clearSelection();
                } else if (e.getSource() == membershipTable) {
                    propertyTable.clearSelection();
                }
            }
            else if (e.getSource() == propertyTable)
            {
                int row = propertyTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    propertyEdit(row);
                }
            } else if (e.getSource() == membershipTable) {
                int row = membershipTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    membershipEdit(row);
                }
            }
            else
            {
                add();
            }
        }
        @Override public void mousePressed(MouseEvent e) {
            if (e.getSource() == propertyTable) {
                membershipTable.clearSelection();
            } else if (e.getSource() == membershipTable) {
                propertyTable.clearSelection();
            }
        }
    }

    // hook for roadsigns plugin to display a small
    // button in the upper right corner of this dialog
    public static JPanel pluginHook = new JPanel();

    private final Map<String, Map<String, Integer>> valueCount = new TreeMap<String, Map<String, Integer>>();

    Comparator<AutoCompletionListItem> defaultACItemComparator = new Comparator<AutoCompletionListItem>() {
        public int compare(AutoCompletionListItem o1, AutoCompletionListItem o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
        }
    };

    private DataSetListenerAdapter dataChangedAdapter = new DataSetListenerAdapter(this);
    private HelpAction helpAction = new HelpAction();
    private AddAction addAction = new AddAction();
    private Shortcut addActionShortcut = Shortcut.registerShortcut("properties:add", tr("Add Properties"), KeyEvent.VK_B,
            Shortcut.GROUP_MNEMONIC);

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(dataChangedAdapter, FireMode.IN_EDT_CONSOLIDATED);
        SelectionEventManager.getInstance().addSelectionListener(this, FireMode.IN_EDT_CONSOLIDATED);
        MapView.addEditLayerChangeListener(this);
        updateSelection();
        Main.registerActionShortcut(addAction, addActionShortcut);
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(dataChangedAdapter);
        SelectionEventManager.getInstance().removeSelectionListener(this);
        MapView.removeEditLayerChangeListener(this);
        Main.unregisterActionShortcut(addAction, addActionShortcut);
    }

    /**
     * Edit the value in the properties table row
     * @param row The row of the table from which the value is edited.
     */
    @SuppressWarnings("unchecked")
    void propertyEdit(int row) {
        Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
        if (sel.isEmpty()) return;

        String key = propertyData.getValueAt(row, 0).toString();
        objKey=key;

        String msg = "<html>"+trn("This will change {0} object.",
                "This will change up to {0} objects.", sel.size(), sel.size())
                +"<br><br>("+tr("An empty value deletes the tag.", key)+")</html>";

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(msg), BorderLayout.NORTH);

        JPanel p = new JPanel(new GridBagLayout());
        panel.add(p, BorderLayout.CENTER);

        AutoCompletionManager autocomplete = Main.main.getEditLayer().data.getAutoCompletionManager();
        List<AutoCompletionListItem> keyList = autocomplete.getKeys();
        Collections.sort(keyList, defaultACItemComparator);

        final AutoCompletingComboBox keys = new AutoCompletingComboBox();
        keys.setPossibleACItems(keyList);
        keys.setEditable(true);
        keys.setSelectedItem(key);

        p.add(new JLabel(tr("Key")), GBC.std());
        p.add(Box.createHorizontalStrut(10), GBC.std());
        p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));

        final AutoCompletingComboBox values = new AutoCompletingComboBox();
        values.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,  boolean cellHasFocus){
                Component c = super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    String str = null;
                    str=((AutoCompletionListItem) value).getValue();
                    if (valueCount.containsKey(objKey)){
                        Map<String, Integer> m=valueCount.get(objKey);
                        if (m.containsKey(str)) {
                            str+="("+m.get(str)+")";
                            c.setFont(c.getFont().deriveFont(Font.ITALIC+Font.BOLD));
                        }
                    }
                    ((JLabel)c).setText(str);
                }
                return c;
            }
        });
        values.setEditable(true);

        List<AutoCompletionListItem> valueList = autocomplete.getValues(key);
        Collections.sort(valueList, defaultACItemComparator);

        values.setPossibleACItems(valueList);
        Map<String, Integer> m=(Map<String, Integer>)propertyData.getValueAt(row, 1);
        final String selection= m.size()!=1?tr("<different>"):m.entrySet().iterator().next().getKey();
        values.setSelectedItem(selection);
        values.getEditor().setItem(selection);
        p.add(new JLabel(tr("Value")), GBC.std());
        p.add(Box.createHorizontalStrut(10), GBC.std());
        p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
        addFocusAdapter(row, keys, values, autocomplete);

        final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            @Override public void selectInitialValue() {
                values.requestFocusInWindow();
                values.getEditor().selectAll();
            }
        };
        final JDialog dlg = optionPane.createDialog(Main.parent, tr("Change values?"));
        dlg.setModalityType(ModalityType.DOCUMENT_MODAL);

        values.getEditor().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });

        String oldValue = values.getEditor().getItem().toString();
        dlg.setVisible(true);

        Object answer = optionPane.getValue();
        if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
                (answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION)) {
            values.getEditor().setItem(oldValue);
            return;
        }

        String value = values.getEditor().getItem().toString().trim();
        // is not Java 1.5
        //value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC);
        if (value.equals("")) {
            value = null; // delete the key
        }
        String newkey = keys.getEditor().getItem().toString().trim();
        //newkey = java.text.Normalizer.normalize(newkey, java.text.Normalizer.Form.NFC);
        if (newkey.equals("")) {
            newkey = key;
            value = null; // delete the key instead
        }
        if (key.equals(newkey) && tr("<different>").equals(value))
            return;
        if (key.equals(newkey) || value == null) {
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, newkey, value));
        } else {
            Collection<Command> commands=new Vector<Command>();
            commands.add(new ChangePropertyCommand(sel, key, null));
            if (value.equals(tr("<different>"))) {
                HashMap<String, Vector<OsmPrimitive>> map=new HashMap<String, Vector<OsmPrimitive>>();
                for (OsmPrimitive osm: sel) {
                    String val=osm.get(key);
                    if(val != null)
                    {
                        if (map.containsKey(val)) {
                            map.get(val).add(osm);
                        } else {
                            Vector<OsmPrimitive> v = new Vector<OsmPrimitive>();
                            v.add(osm);
                            map.put(val, v);
                        }
                    }
                }
                for (Entry<String, Vector<OsmPrimitive>> e: map.entrySet()) {
                    commands.add(new ChangePropertyCommand(e.getValue(), newkey, e.getKey()));
                }
            } else {
                commands.add(new ChangePropertyCommand(sel, newkey, value));
            }
            Main.main.undoRedo.add(new SequenceCommand(
                    trn("Change properties of up to {0} object",
                            "Change properties of up to {0} objects", sel.size(), sel.size()),
                            commands));
        }

        if(!key.equals(newkey)) {
            for(int i=0; i < propertyTable.getRowCount(); i++)
                if(propertyData.getValueAt(i, 0).toString().equals(newkey)) {
                    row=i;
                    break;
                }
        }
        propertyTable.changeSelection(row, 0, false, false);
    }

    /**
     * This simply fires up an relation editor for the relation shown; everything else
     * is the editor's business.
     *
     * @param row
     */
    void membershipEdit(int row) {
        Relation relation = (Relation)membershipData.getValueAt(row, 0);
        Main.map.relationListDialog.selectRelation(relation);
        RelationEditor.getEditor(
                Main.map.mapView.getEditLayer(),
                relation,
                ((MemberInfo) membershipData.getValueAt(row, 1)).role).setVisible(true);
    }

    /**
     * Open the add selection dialog and add a new key/value to the table (and
     * to the dataset, of course).
     */
    void add() {
        Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
        if (sel.isEmpty()) return;

        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("<html>"+trn("This will change up to {0} object.",
                "This will change up to {0} objects.", sel.size(),sel.size())
                +"<br><br>"+tr("Please select a key")), BorderLayout.NORTH);
        final AutoCompletingComboBox keys = new AutoCompletingComboBox();
        AutoCompletionManager autocomplete = Main.main.getEditLayer().data.getAutoCompletionManager();
        List<AutoCompletionListItem> keyList = autocomplete.getKeys();

        // remove the object's tag keys from the list
        Iterator<AutoCompletionListItem> iter = keyList.iterator();
        while (iter.hasNext()) {
            AutoCompletionListItem item = iter.next();
            for (int i = 0; i < propertyData.getRowCount(); ++i) {
                if (item.getValue().equals(propertyData.getValueAt(i, 0))) {
                    iter.remove();
                    break;
                }
            }
        }

        Collections.sort(keyList, defaultACItemComparator);
        keys.setPossibleACItems(keyList);
        keys.setEditable(true);

        p.add(keys, BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout());
        p.add(p2, BorderLayout.SOUTH);
        p2.add(new JLabel(tr("Please select a value")), BorderLayout.NORTH);
        final AutoCompletingComboBox values = new AutoCompletingComboBox();
        values.setEditable(true);
        p2.add(values, BorderLayout.CENTER);

        FocusAdapter focus = addFocusAdapter(-1, keys, values, autocomplete);
        // fire focus event in advance or otherwise the popup list will be too small at first
        focus.focusGained(null);

        JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
            @Override public void selectInitialValue() {
                keys.requestFocusInWindow();
                keys.getEditor().selectAll();
            }
        };
        JDialog dialog = pane.createDialog(Main.parent, tr("Change values?"));
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);

        if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
            return;
        String key = keys.getEditor().getItem().toString().trim();
        String value = values.getEditor().getItem().toString().trim();
        if (value.equals(""))
            return;
        Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, value));
        btnAdd.requestFocusInWindow();
    }

    /**
     * @param allData
     * @param keys
     * @param values
     */
    private FocusAdapter addFocusAdapter(final int row, final AutoCompletingComboBox keys, final AutoCompletingComboBox values, final AutoCompletionManager autocomplete) {
        // get the combo box' editor component
        JTextComponent editor = (JTextComponent)values.getEditor()
        .getEditorComponent();
        // Refresh the values model when focus is gained
        FocusAdapter focus = new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                String key = keys.getEditor().getItem().toString();

                List<AutoCompletionListItem> valueList = autocomplete.getValues(key);
                Collections.sort(valueList, defaultACItemComparator);

                values.setPossibleACItems(valueList);
                objKey=key;
            }
        };
        editor.addFocusListener(focus);
        return focus;
    }
    private String objKey;

    /**
     * The property data.
     */
    private final DefaultTableModel propertyData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    /**
     * The membership data.
     */
    private final DefaultTableModel membershipData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    /**
     * The properties list.
     */
    private final JTable propertyTable = new JTable(propertyData);
    private final JTable membershipTable = new JTable(membershipData);

    public JComboBox taggingPresets = new JComboBox();

    /**
     * The Add/Edit/Delete buttons (needed to be able to disable them)
     */
    private final SideButton btnAdd;
    private final SideButton btnEdit;
    private final SideButton btnDel;
    private final PresetListPanel presets = new PresetListPanel();

    private final JLabel selectSth = new JLabel("<html><p>"
            + tr("Please select the objects you want to change properties for.") + "</p></html>");

    static class MemberInfo {
        List<RelationMember> role = new ArrayList<RelationMember>();
        List<Integer> position = new ArrayList<Integer>();
        private String positionString = null;
        void add(RelationMember r, Integer p)
        {
            role.add(r);
            position.add(p);
        }
        String getPositionString()
        {
            if(positionString == null)
            {
                Collections.sort(position);
                positionString = String.valueOf(position.get(0));
                int cnt = 0;
                int last = position.get(0);
                for(int i = 1; i < position.size(); ++i) {
                    int cur = position.get(i);
                    if(cur == last+1) {
                        ++cnt;
                    } else {
                        if(cnt == 1) {
                            positionString += ","+String.valueOf(last);
                        } else if(cnt > 1) {
                            positionString += "-"+String.valueOf(last);
                        }
                        positionString += "-"+String.valueOf(cur);
                        cnt = 0;
                    }
                    last = cur;
                }
                if(cnt == 1) {
                    positionString += ","+String.valueOf(last);
                } else if(cnt > 1) {
                    positionString += "-"+String.valueOf(last);
                }
            }
            if(positionString.length() > 20) {
                positionString = positionString.substring(0,17)+"...";
            }
            return positionString;
        }
    }

    /**
     * Create a new PropertiesDialog
     */
    public PropertiesDialog(MapFrame mapFrame) {
        super(tr("Properties/Memberships"), "propertiesdialog", tr("Properties for selected objects."),
                Shortcut.registerShortcut("subwindow:properties", tr("Toggle: {0}", tr("Properties/Memberships")), KeyEvent.VK_P,
                        Shortcut.GROUP_LAYER, Shortcut.SHIFT_DEFAULT), 150, true);

        // setting up the properties table
        propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
        propertyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        propertyTable.addMouseListener(new PopupMenuLauncher() {
            @Override
            public void launch(MouseEvent evt) {
                Point p = evt.getPoint();
                int row = propertyTable.rowAtPoint(p);
                if (row > -1) {
                    propertyTable.changeSelection(row, 0, false, false);
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(helpAction);
                    menu.show(propertyTable, p.x, p.y-3);
                }
            }
        });

        propertyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (c instanceof JLabel) {
                    String str = null;
                    if (value instanceof String) {
                        str = (String) value;
                    } else if (value instanceof Map<?, ?>) {
                        Map<?, ?> v = (Map<?, ?>) value;
                        if (v.size() != 1) {
                            str=tr("<different>");
                            c.setFont(c.getFont().deriveFont(Font.ITALIC));
                        } else {
                            final Map.Entry<?, ?> entry = v.entrySet().iterator().next();
                            str = (String) entry.getKey();
                        }
                    }
                    ((JLabel)c).setText(str);
                }
                return c;
            }
        });

        // setting up the membership table

        membershipData.setColumnIdentifiers(new String[]{tr("Member Of"),tr("Role"),tr("Position")});
        membershipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        membershipTable.addMouseListener(new PopupMenuLauncher() {
            @Override
            public void launch(MouseEvent evt) {
                Point p = evt.getPoint();
                int row = membershipTable.rowAtPoint(p);
                if (row > -1) {
                    membershipTable.changeSelection(row, 0, false, false);
                    JPopupMenu menu = new JPopupMenu();
                    Relation relation = (Relation)membershipData.getValueAt(row, 0);
                    menu.add(new SelectRelationAction(relation, true));
                    menu.add(new SelectRelationAction(relation, false));
                    menu.addSeparator();
                    menu.add(helpAction);
                    menu.show(membershipTable, p.x, p.y-3);
                }
            }
        });

        TableColumnModel mod = membershipTable.getColumnModel();
        mod.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    Relation r = (Relation)value;
                    label.setText(r.getDisplayName(DefaultNameFormatter.getInstance()));
                    if (r.isDisabledAndHidden()) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    MemberInfo col = (MemberInfo) value;

                    String text = null;
                    for (RelationMember r : col.role) {
                        if (text == null) {
                            text = r.getRole();
                        }
                        else if (!text.equals(r.getRole())) {
                            text = tr("<different>");
                            break;
                        }
                    }

                    label.setText(text);
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    label.setText(((MemberInfo) table.getValueAt(row, 1)).getPositionString());
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });
        mod.getColumn(2).setPreferredWidth(20);
        mod.getColumn(1).setPreferredWidth(40);
        mod.getColumn(0).setPreferredWidth(200);

        // combine both tables and wrap them in a scrollPane
        JPanel bothTables = new JPanel();
        boolean top = Main.pref.getBoolean("properties.presets.top", true);
        bothTables.setLayout(new GridBagLayout());
        if(top) {
            bothTables.add(presets, GBC.std().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2).anchor(GBC.NORTHWEST));
            double epsilon = Double.MIN_VALUE; // need to set a weight or else anchor value is ignored
            bothTables.add(pluginHook, GBC.eol().insets(0,1,1,1).anchor(GBC.NORTHEAST).weight(epsilon, epsilon));
        }
        bothTables.add(selectSth, GBC.eol().fill().insets(10, 10, 10, 10));
        bothTables.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(propertyTable, GBC.eol().fill(GBC.BOTH));
        bothTables.add(membershipTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(membershipTable, GBC.eol().fill(GBC.BOTH));
        if(!top) {
            bothTables.add(presets, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2));
        }

        DblClickWatch dblClickWatch = new DblClickWatch();
        propertyTable.addMouseListener(dblClickWatch);
        membershipTable.addMouseListener(dblClickWatch);
        JScrollPane scrollPane = new JScrollPane(bothTables);
        scrollPane.addMouseListener(dblClickWatch);
        add(scrollPane, BorderLayout.CENTER);

        selectSth.setPreferredSize(scrollPane.getSize());
        presets.setSize(scrollPane.getSize());

        JPanel buttonPanel = getButtonPanel(3);

        // -- add action and shortcut
        this.btnAdd = new SideButton(addAction);
        btnAdd.setFocusable(true);
        buttonPanel.add(this.btnAdd);
        btnAdd.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "onEnter");
        btnAdd.getActionMap().put("onEnter", addAction);

        // -- edit action
        //
        EditAction editAction = new EditAction();
        propertyTable.getSelectionModel().addListSelectionListener(editAction);
        membershipTable.getSelectionModel().addListSelectionListener(editAction);
        this.btnEdit = new SideButton(editAction);
        buttonPanel.add(this.btnEdit);

        // -- delete action
        //
        DeleteAction deleteAction = new DeleteAction();
        this.btnDel = new SideButton(deleteAction);
        membershipTable.getSelectionModel().addListSelectionListener(deleteAction);
        propertyTable.getSelectionModel().addListSelectionListener(deleteAction);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),"delete"
        );
        getActionMap().put("delete", deleteAction);
        buttonPanel.add(this.btnDel);
        add(buttonPanel, BorderLayout.SOUTH);

        // -- help action
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "onHelp");
        getActionMap().put("onHelp", helpAction);
    }

    @Override public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    private int findRow(TableModel model, Object value) {
        for (int i=0; i<model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(value))
                return i;
        }
        return -1;
    }

    private PresetHandler presetHandler = new PresetHandler() {

        @Override
        public void updateTags(List<Tag> tags) {
            Command command = TaggingPreset.createCommand(getSelection(), tags);
            if (command != null) {
                Main.main.undoRedo.add(command);
            }
        }

        @Override
        public Collection<OsmPrimitive> getSelection() {
            if (Main.main == null) return null;
            if (Main.main.getCurrentDataSet() == null) return null;

            return Main.main.getCurrentDataSet().getSelected();
        }
    };

    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (!isVisible())
            return;
        if (propertyTable == null)
            return; // selection changed may be received in base class constructor before init
        if (propertyTable.getCellEditor() != null) {
            propertyTable.getCellEditor().cancelCellEditing();
        }

        String selectedTag = null;
        Relation selectedRelation = null;
        if (propertyTable.getSelectedRowCount() == 1) {
            selectedTag = (String)propertyData.getValueAt(propertyTable.getSelectedRow(), 0);
        }
        if (membershipTable.getSelectedRowCount() == 1) {
            selectedRelation = (Relation)membershipData.getValueAt(membershipTable.getSelectedRow(), 0);
        }

        // re-load property data
        propertyData.setRowCount(0);
        int nodes = 0;
        int ways = 0;
        int relations = 0;
        int closedways = 0;

        Map<String, Integer> keyCount = new HashMap<String, Integer>();
        valueCount.clear();
        for (OsmPrimitive osm : newSelection) {
            if(osm instanceof Node) {
                ++nodes;
            } else if(osm instanceof Relation) {
                ++relations;
            } else if(((Way)osm).isClosed()) {
                ++closedways;
            } else {
                ++ways;
            }
            for (String key: osm.keySet()) {
                String value = osm.get(key);
                keyCount.put(key, keyCount.containsKey(key) ? keyCount.get(key) + 1 : 1);
                if (valueCount.containsKey(key)) {
                    Map<String, Integer> v = valueCount.get(key);
                    v.put(value, v.containsKey(value)? v.get(value) + 1 : 1 );
                } else {
                    TreeMap<String,Integer> v = new TreeMap<String, Integer>();
                    v.put(value, 1);
                    valueCount.put(key, v);
                }
            }
        }
        for (Entry<String, Map<String, Integer>> e : valueCount.entrySet()) {
            int count=0;
            for (Entry<String, Integer> e1: e.getValue().entrySet()) {
                count+=e1.getValue();
            }
            if (count < newSelection.size()) {
                e.getValue().put("", newSelection.size()-count);
            }
            propertyData.addRow(new Object[]{e.getKey(), e.getValue()});
        }

        membershipData.setRowCount(0);

        Map<Relation, MemberInfo> roles = new HashMap<Relation, MemberInfo>();
        for (OsmPrimitive primitive: newSelection) {
            for (OsmPrimitive ref: primitive.getReferrers()) {
                if (ref instanceof Relation && !ref.isIncomplete() && !ref.isDeleted()) {
                    Relation r = (Relation) ref;
                    MemberInfo mi = roles.get(r);
                    if(mi == null) {
                        mi = new MemberInfo();
                    }
                    roles.put(r, mi);
                    int i = 1;
                    for (RelationMember m : r.getMembers()) {
                        if (m.getMember() == primitive) {
                            mi.add(m, i);
                        }
                        ++i;
                    }
                }
            }
        }

        List<Relation> sortedRelations = new ArrayList<Relation>(roles.keySet());
        Collections.sort(sortedRelations, new Comparator<Relation>() {
            public int compare(Relation o1, Relation o2) {
                int comp = Boolean.valueOf(o1.isDisabledAndHidden()).compareTo(o2.isDisabledAndHidden());
                if (comp == 0) {
                    comp = o1.getDisplayName(DefaultNameFormatter.getInstance()).compareTo(o2.getDisplayName(DefaultNameFormatter.getInstance()));
                }
                return comp;
            }}
        );

        for (Relation r: sortedRelations) {
            membershipData.addRow(new Object[]{r, roles.get(r)});
        }

        presets.updatePresets(nodes, ways, relations, closedways, valueCount, presetHandler);

        membershipTable.getTableHeader().setVisible(membershipData.getRowCount() > 0);
        membershipTable.setVisible(membershipData.getRowCount() > 0);

        boolean hasSelection = !newSelection.isEmpty();
        boolean hasTags = hasSelection && propertyData.getRowCount() > 0;
        boolean hasMemberships = hasSelection && membershipData.getRowCount() > 0;
        btnAdd.setEnabled(hasSelection);
        btnEdit.setEnabled(hasTags || hasMemberships);
        btnDel.setEnabled(hasTags || hasMemberships);
        propertyTable.setVisible(hasTags);
        propertyTable.getTableHeader().setVisible(hasTags);
        selectSth.setVisible(!hasSelection);
        pluginHook.setVisible(hasSelection);

        int selectedIndex;
        if (selectedTag != null && (selectedIndex = findRow(propertyData, selectedTag)) != -1) {
            propertyTable.changeSelection(selectedIndex, 0, false, false);
        } else if (selectedRelation != null && (selectedIndex = findRow(membershipData, selectedRelation)) != -1) {
            membershipTable.changeSelection(selectedIndex, 0, false, false);
        } else if(hasTags) {
            propertyTable.changeSelection(0, 0, false, false);
        } else if(hasMemberships) {
            membershipTable.changeSelection(0, 0, false, false);
        }

        if(propertyData.getRowCount() != 0 || membershipData.getRowCount() != 0) {
            setTitle(tr("Properties: {0} / Memberships: {1}",
                    propertyData.getRowCount(), membershipData.getRowCount()));
        } else {
            setTitle(tr("Properties / Memberships"));
        }
    }

    private void updateSelection() {
        if (Main.main.getCurrentDataSet() == null) {
            selectionChanged(Collections.<OsmPrimitive>emptyList());
        } else {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* EditLayerChangeListener                                                                */
    /* ---------------------------------------------------------------------------------- */
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        updateSelection();
    }

    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        updateSelection();
    }

    class DeleteAction extends AbstractAction implements ListSelectionListener {

        protected void deleteProperty(int row){
            String key = propertyData.getValueAt(row, 0).toString();

            String nextKey = null;
            int rowCount = propertyData.getRowCount();
            if (rowCount > 1) {
                nextKey = (String)propertyData.getValueAt((row + 1 < rowCount ? row + 1 : row - 1), 0);
            }

            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, null));

            membershipTable.clearSelection();
            if (nextKey != null) {
                propertyTable.changeSelection(findRow(propertyData, nextKey), 0, false, false);
            }
        }

        protected void deleteFromRelation(int row) {
            Relation cur = (Relation)membershipData.getValueAt(row, 0);

            Relation nextRelation = null;
            int rowCount = membershipTable.getRowCount();
            if (rowCount > 1) {
                nextRelation = (Relation)membershipData.getValueAt((row + 1 < rowCount ? row + 1 : row - 1), 0);
            }

            ExtendedDialog ed = new ExtendedDialog(Main.parent,
                    tr("Change relation"),
                    new String[] {tr("Delete from relation"), tr("Cancel")});
            ed.setButtonIcons(new String[] {"dialogs/delete.png", "cancel.png"});
            ed.setContent(tr("Really delete selection from relation {0}?", cur.getDisplayName(DefaultNameFormatter.getInstance())));
            ed.showDialog();

            if(ed.getValue() != 1)
                return;

            Relation rel = new Relation(cur);
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            for (OsmPrimitive primitive: sel) {
                rel.removeMembersFor(primitive);
            }
            Main.main.undoRedo.add(new ChangeCommand(cur, rel));

            propertyTable.clearSelection();
            if (nextRelation != null) {
                membershipTable.changeSelection(findRow(membershipData, nextRelation), 0, false, false);
            }
        }

        public DeleteAction() {
            putValue(NAME, tr("Delete"));
            putValue(SHORT_DESCRIPTION, tr("Delete the selected key in all objects"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "delete"));
            Shortcut s = Shortcut.registerShortcut("properties:delete", tr("Delete Properties"), KeyEvent.VK_Q,
                    Shortcut.GROUP_MNEMONIC);
            putValue(MNEMONIC_KEY, (int) KeyEvent.getKeyText(s.getAssignedKey()).charAt(0));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (propertyTable.getSelectedRowCount() >0 ) {
                int row = propertyTable.getSelectedRow();
                deleteProperty(row);
            } else if (membershipTable.getSelectedRowCount() > 0) {
                int row = membershipTable.getSelectedRow();
                deleteFromRelation(row);
            }
        }

        protected void updateEnabledState() {
            setEnabled(
                    PropertiesDialog.this.propertyTable.getSelectedRowCount() >0
                    || PropertiesDialog.this.membershipTable.getSelectedRowCount() > 0
            );
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class AddAction extends AbstractAction {
        public AddAction() {
            putValue(NAME, tr("Add"));
            putValue(SHORT_DESCRIPTION, tr("Add a new key/value pair to all objects"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "add"));
        }

        public void actionPerformed(ActionEvent e) {
            add();
        }
    }

    class EditAction extends AbstractAction implements ListSelectionListener {
        public EditAction() {
            putValue(NAME, tr("Edit"));
            putValue(SHORT_DESCRIPTION, tr("Edit the value of the selected key for all objects"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "edit"));
            updateEnabledState();
        }

        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            if (propertyTable.getSelectedRowCount() == 1) {
                int row = propertyTable.getSelectedRow();
                propertyEdit(row);
            } else if (membershipTable.getSelectedRowCount() == 1) {
                int row = membershipTable.getSelectedRow();
                membershipEdit(row);
            }
        }

        protected void updateEnabledState() {
            setEnabled(
                    propertyTable.getSelectedRowCount() == 1
                    ^ membershipTable.getSelectedRowCount() == 1
            );
        }

        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class HelpAction extends AbstractAction {
        public HelpAction() {
            putValue(NAME, tr("Go to OSM wiki for tag help (F1)"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with wiki help to selected object"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "search"));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                String base = Main.pref.get("url.openstreetmap-wiki", "http://wiki.openstreetmap.org/wiki/");
                String lang = LanguageInfo.getWikiLanguagePrefix();
                final List<URI> uris = new ArrayList<URI>();
                int row;
                if (propertyTable.getSelectedRowCount() == 1) {
                    row = propertyTable.getSelectedRow();
                    String key = URLEncoder.encode(propertyData.getValueAt(row, 0).toString(), "UTF-8");
                    String val = URLEncoder.encode(
                            ((Map<String,Integer>)propertyData.getValueAt(row, 1))
                            .entrySet().iterator().next().getKey(), "UTF-8"
                    );

                    uris.add(new URI(String.format("%s%sTag:%s=%s", base, lang, key, val)));
                    uris.add(new URI(String.format("%sTag:%s=%s", base, key, val)));
                    uris.add(new URI(String.format("%s%sKey:%s", base, lang, key)));
                    uris.add(new URI(String.format("%sKey:%s", base, key)));
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                } else if (membershipTable.getSelectedRowCount() == 1) {
                    row = membershipTable.getSelectedRow();
                    String type = URLEncoder.encode(
                            ((Relation)membershipData.getValueAt(row, 0)).get("type"), "UTF-8"
                    );

                    if (type != null && !type.equals("")) {
                        uris.add(new URI(String.format("%s%sRelation:%s", base, lang, type)));
                        uris.add(new URI(String.format("%sRelation:%s", base, type)));
                    }

                    uris.add(new URI(String.format("%s%sRelations", base, lang)));
                    uris.add(new URI(String.format("%sRelations", base)));
                } else {
                    // give the generic help page, if more than one element is selected
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                }

                Main.worker.execute(new Runnable(){
                    public void run() {
                        try {
                            // find a page that actually exists in the wiki
                            HttpURLConnection conn;
                            for (URI u : uris) {
                                conn = (HttpURLConnection) u.toURL().openConnection();
                                conn.setConnectTimeout(5000);

                                if (conn.getResponseCode() != 200) {
                                    System.out.println("INFO: " + u + " does not exist");
                                    conn.disconnect();
                                } else {
                                    int osize = conn.getContentLength();
                                    conn.disconnect();

                                    conn = (HttpURLConnection) new URI(u.toString()
                                            .replace("=", "%3D") /* do not URLencode whole string! */
                                            .replaceFirst("/wiki/", "/w/index.php?redirect=no&title=")
                                    ).toURL().openConnection();
                                    conn.setConnectTimeout(5000);

                                    /* redirect pages have different content length, but retrieving a "nonredirect"
                                     *  page using index.php and the direct-link method gives slightly different
                                     *  content lengths, so we have to be fuzzy.. (this is UGLY, recode if u know better)
                                     */
                                    if (Math.abs(conn.getContentLength() - osize) > 200) {
                                        System.out.println("INFO: " + u + " is a mediawiki redirect");
                                        conn.disconnect();
                                    } else {
                                        System.out.println("INFO: browsing to " + u);
                                        conn.disconnect();

                                        OpenBrowser.displayUrl(u.toString());
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    static class SelectRelationAction extends AbstractAction {
        boolean selectionmode;
        Relation relation;
        public SelectRelationAction(Relation r, boolean select) {
            selectionmode = select;
            relation = r;
            if(select) {
                putValue(NAME, tr("Select relation"));
                putValue(SHORT_DESCRIPTION, tr("Select relation in main selection."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            } else {
                putValue(NAME, tr("Select in relation list"));
                putValue(SHORT_DESCRIPTION, tr("Select relation in relation list."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "relationlist"));
            }
        }

        public void actionPerformed(ActionEvent e) {
            if(selectionmode) {
                Main.map.mapView.getEditLayer().data.setSelected(relation);
            } else {
                Main.map.relationListDialog.selectRelation(relation);
            }
        }
    }
}
