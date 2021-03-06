// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;

public class TagConflictResolverTable extends JTable implements MultiValueCellEditor.NavigationListener {

    private SelectNextColumnCellAction selectNextColumnCellAction;
    private SelectPreviousColumnCellAction selectPreviousColumnCellAction;

    public TagConflictResolverTable(TagConflictResolverModel model) {
        super(model, new TagConflictResolverColumnModel());
        build();
    }

    protected void build() {
        setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // make ENTER behave like TAB
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false), "selectNextColumnCell");

        // install custom navigation actions
        //
        selectNextColumnCellAction = new SelectNextColumnCellAction();
        selectPreviousColumnCellAction = new SelectPreviousColumnCellAction();
        getActionMap().put("selectNextColumnCell", selectNextColumnCellAction);
        getActionMap().put("selectPreviousColumnCell", selectPreviousColumnCellAction);

        ((MultiValueCellEditor)getColumnModel().getColumn(2).getCellEditor()).addNavigationListeners(this);

        setRowHeight((int)new JComboBox().getPreferredSize().getHeight());
    }

    /**
     * Action to be run when the user navigates to the next cell in the table, for instance by
     * pressing TAB or ENTER. The action alters the standard navigation path from cell to cell: <ul>
     * <li>it jumps over cells in the first column</li> <li>it automatically add a new empty row
     * when the user leaves the last cell in the table</li> <ul>
     *
     *
     */
    class SelectNextColumnCellAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void run() {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col == 2 && row < getRowCount() - 1) {
                row++;
            } else if (row < getRowCount() - 1) {
                col = 2;
                row++;
            }
            changeSelection(row, col, false, false);
            editCellAt(getSelectedRow(), getSelectedColumn());
            getEditorComponent().requestFocusInWindow();
        }
    }

    /**
     * Action to be run when the user navigates to the previous cell in the table, for instance by
     * pressing Shift-TAB
     *
     */
    class SelectPreviousColumnCellAction extends AbstractAction {

        public void actionPerformed(ActionEvent e) {
            run();
        }

        public void run() {
            int col = getSelectedColumn();
            int row = getSelectedRow();
            if (getCellEditor() != null) {
                getCellEditor().stopCellEditing();
            }

            if (col <= 0 && row <= 0) {
                // change nothing
            } else if (row > 0) {
                col = 2;
                row--;
            }
            changeSelection(row, col, false, false);
            editCellAt(getSelectedRow(), getSelectedColumn());
            getEditorComponent().requestFocusInWindow();
        }
    }

    public void gotoNextDecision() {
        selectNextColumnCellAction.run();
    }

    public void gotoPreviousDecision() {
        selectPreviousColumnCellAction.run();
    }
}
