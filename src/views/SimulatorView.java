package views;

import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class SimulatorView {

	private JFrame frame;

	/**
	 * Create the application.
	 */
	public SimulatorView(Map<Long, Integer> data) {
		initialize(data);
		frame.setVisible(true);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(Map<Long, Integer> data) {
		frame = new JFrame();
		frame.setBounds(100, 100, 1600, 500);

		XYSeriesCollection dataset = new XYSeriesCollection();
		
		XYSeries series = new XYSeries("txwnd/MSS");
		for (Entry<Long, Integer> iterable_element : data.entrySet()) {
			series.add(iterable_element.getKey(), iterable_element.getValue());
		}
		dataset.addSeries(series);
		JFreeChart chart = ChartFactory.createXYLineChart(
				"Gráfico do simulador", "Tempo", "Valor", dataset,
				PlotOrientation.VERTICAL, true, true, false);

		ChartPanel chartPanel = new ChartPanel(chart);
		frame.add(chartPanel);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
