package br.ufmg.dcc.labsoft.java.jmove.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Date;

import br.ufmg.dcc.labsoft.java.jmove.util.DateUtil;

public class LogSystem {

	private static String logAddres = "_error.log";
	private static PrintStream output;

	public static void write(Throwable error) {

		try {
			if (output == null) {
				output = new PrintStream(new File(logAddres));
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();

		}

		if (output != null) {
			output.print(getCustomStackTrace(error));
		}
	}

	private static String getCustomStackTrace(Throwable aThrowable) {
		// add the class name and any message passed to constructor
		final StringBuilder result = new StringBuilder(DateUtil.dateToStr(
				new Date(), "yyyyMMdd-HH:mm:ss" + " : "));
		result.append(aThrowable.toString());
		final String NEW_LINE = System.getProperty("line.separator");
		result.append(NEW_LINE);

		// add each element of the stack trace
		for (StackTraceElement element : aThrowable.getStackTrace()) {
			result.append(element);
			result.append(NEW_LINE);
		}
		return result.toString();
	}

	public static void finish() {

		if (output != null) {
			output.close();
		}

	}
}
