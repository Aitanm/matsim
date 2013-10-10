package josmMatsimPlugin;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.Layer;

/**
 * the main MATSim-dialog
 * @author nkuehnel
 * 
 */
public class MATSimDialog extends JPanel
{

	// the JOptionPane that contains this dialog. required for the closeDialog()
	// method.
	private JButton importButton;
	private JButton exportButton;
	private JButton newNetButton;

	public MATSimDialog(final JDialog dlgSuper)
	{
		GridBagConstraints c = new GridBagConstraints();

		setLayout(new GridBagLayout());

		c.insets = new Insets(4, 4, 4, 4);
		c.gridwidth = 1;
		c.weightx = 0.8;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		add(new JLabel(tr("Choose:")), c);

		importButton = new JButton("Import network");

		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 1.5;
		add(importButton, c);

		importButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dlgSuper.dispose();
				MATSimImportDialog dialog = new MATSimImportDialog();
		        JOptionPane pane = new JOptionPane(dialog, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		        dialog.setOptionPane(pane);
		        JDialog dlg = pane.createDialog(Main.parent, tr("Import"));
		        dlg.setAlwaysOnTop(true);
		        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		      
		        dlg.setVisible(true);
		        if(pane.getValue()!=null)
		        {
		        	if(((Integer)pane.getValue()) == JOptionPane.OK_OPTION)
		        	{
		        		ImportTask task = new ImportTask();
		        		Main.worker.execute(task);
		        	}
		        }
		        dlg.dispose();
		       
			}
		});

		exportButton = new JButton("Export network");
		
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 1.5;
		add(exportButton, c);

		exportButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dlgSuper.dispose();
				ExportDefaults.initialize();
				MATSimExportDialog dialog = new MATSimExportDialog();
		        JOptionPane pane = new JOptionPane(dialog, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
		        dialog.setOptionPane(pane);
		        JDialog dlg = pane.createDialog(Main.parent, tr("Export"));
		        dlg.setAlwaysOnTop(true);
		        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		        dlg.setVisible(true);
		        if(pane.getValue()!=null)
		        {
		        	if(((Integer)pane.getValue()) == JOptionPane.OK_OPTION)
		        	{
		        		ExportTask task = new ExportTask();
		        		Main.worker.execute(task);
		        	}
		        }
		        dlg.dispose();
		        
			}
		});
		
		newNetButton = new JButton("new network layer");
		
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 1;
		c.weightx = 1.5;
		add(newNetButton, c);
		
		newNetButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dlgSuper.dispose();
				DataSet dataSet = new DataSet();
				Layer layer = new NetworkLayer(dataSet, "new Layer", new File("new Layer"));
				Main.main.addLayer(layer);
		        
			}
		});
	}
}
