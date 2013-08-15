package Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import Controller.Simulator;
 
public class WriteToFile {
	
	private static WriteToFile instance = null;
	
	private BufferedWriter bw = null;

	public WriteToFile() throws IOException {

		File file = new File("Estatísticas_Simulador.txt");
		
		if (!file.exists()) {
			file.createNewFile();
		}
		
		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		bw = new BufferedWriter(fw);
		bw.write("================================================================================\n");
		bw.write("				Dados estatísticos da execução do simulador TCP\n");
		bw.write("================================================================================\n");
	}

	public static void writeln(Object line) {
		String text = line.getClass() == String.class? (String)line: line.toString();
		try {
			WriteToFile.getInstance().bw.write(text +"\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void close() throws IOException{
		WriteToFile.getInstance().bw.write("================================================================================\n");
		WriteToFile.getInstance().bw.write("================================================================================\n");
		WriteToFile.getInstance().bw.close();
	}
	
	public static WriteToFile getInstance() throws IOException {
		if (instance == null) {
			instance = new WriteToFile();
		}
		return instance;
	}
}
