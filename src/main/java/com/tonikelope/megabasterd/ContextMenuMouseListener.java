package com.tonikelope.megabasterd;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.text.JTextComponent;

/**
 *
 * @author tonikelope
 */
public final class ContextMenuMouseListener extends MouseAdapter {

    private final JPopupMenu _popup;
    private final Action _cutAction;
    private final Action _copyAction;
    private final Action _pasteAction;
    private final Action _undoAction;
    private final Action _selectAllAction;
    private JTextComponent _textComponent;
    private String _savedString;
    private _Actions _lastActionSelected;

    public ContextMenuMouseListener() {
        _savedString = "";
        _popup = new JPopupMenu();
        _undoAction = new AbstractAction("Undo") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _textComponent.setText("");
                _textComponent.replaceSelection(_savedString);
                _lastActionSelected = _Actions.UNDO;
            }
        };

        _popup.add(_undoAction);
        _popup.addSeparator();
        _cutAction = new AbstractAction("Cut") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _lastActionSelected = _Actions.CUT;
                _savedString = _textComponent.getText();
                _textComponent.cut();
            }
        };

        _popup.add(_cutAction);

        _copyAction = new AbstractAction("Copy") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _lastActionSelected = _Actions.COPY;
                _textComponent.copy();
            }
        };

        _popup.add(_copyAction);

        _pasteAction = new AbstractAction("Paste") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _lastActionSelected = _Actions.PASTE;
                _savedString = _textComponent.getText();
                _textComponent.paste();
            }
        };

        _popup.add(_pasteAction);
        _popup.addSeparator();

        _selectAllAction = new AbstractAction("Select All") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                _lastActionSelected = _Actions.SELECT_ALL;
                _textComponent.selectAll();
            }
        };

        _popup.add(_selectAllAction);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getModifiers() == InputEvent.BUTTON3_MASK) {
            if (!(e.getSource() instanceof JTextComponent)) {

                return;
            }

            _textComponent = (JTextComponent) e.getSource();
            _textComponent.requestFocus();

            boolean enabled = _textComponent.isEnabled();
            boolean editable = _textComponent.isEditable();
            boolean nonempty = !(_textComponent.getText() == null || _textComponent.getText().isEmpty());
            boolean marked = _textComponent.getSelectedText() != null;

            boolean pasteAvailable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).isDataFlavorSupported(DataFlavor.stringFlavor);

            _undoAction.setEnabled(enabled && editable && (_lastActionSelected == _Actions.CUT || _lastActionSelected == _Actions.PASTE));
            _cutAction.setEnabled(enabled && editable && marked);
            _copyAction.setEnabled(enabled && marked);
            _pasteAction.setEnabled(enabled && editable && pasteAvailable);
            _selectAllAction.setEnabled(enabled && nonempty);

            int nx = e.getX();

            if (nx > 500) {
                nx -= _popup.getSize().width;
            }

            _popup.show(e.getComponent(), nx, e.getY() - _popup.getSize().height);
        }
    }

    private enum _Actions {
        UNDO, CUT, COPY, PASTE, SELECT_ALL
    }
    private static final Logger LOG = Logger.getLogger(ContextMenuMouseListener.class.getName());
}
