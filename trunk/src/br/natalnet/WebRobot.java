package br.natalnet;

import android.R.layout;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.app.Dialog;
import android.app.ProgressDialog;

import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;

import java.util.StringTokenizer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Rect;
import android.graphics.YuvImage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class WebRobot extends Activity implements SurfaceHolder.Callback,
		PreviewCallback, OnClickListener {

	private static final int MAX_AVAILABLE = 1;
	private final Semaphore available = new Semaphore(MAX_AVAILABLE, true);
	public int buttonsFunction = 0;
	public static final int BT_DOWNLOAD = 0;
	public static final int BT_CHECK = 1;
	public static final int BT_PYTHON = 2;
	// public static final int BT_ = 1;

	// initialize our progress dialog/bar
	private ProgressDialog mProgressDialog;
	private ProgressDialog mProgressDialogZip;
	public static final int DIALOG_DOWNLOAD_PROGRESS = 0;
	public static final int DIALOG_DOWNLOAD_PROGRESS_ZIP = 1;

	SurfaceHolder mHolder;
	SurfaceView mView;
	Camera mCamera;
	TextView mTextView1;
	private Camera.Parameters parameters;

	private Handler progressBarHandler = new Handler();

	public String fileName = "files.zip";
	public String fileURL = "http://192.168.1.101/files.zip";

	// UI
	public EditText messages;
	public Button YesButton;
	public Button NoButton;
	public TextView statusText;

	boolean okToInit = false;

	private int buttonResult;

	private String lastScript = "";

	PowerManager pm;
	PowerManager.WakeLock wl;

	public String webDirectory;
	public String scriptDirectory;
	private int fileSize;

	boolean downloadDone = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// Para ver imagem ao vivo da câmera
		mView = (SurfaceView) this.findViewById(R.id.CameraView);
		// mTextView1 = (TextView) this.findViewById(R.id.textView1);

		mHolder = mView.getHolder();
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHolder.addCallback(this);

		webDirectory = Environment.getExternalStorageDirectory()
				+ "/WebRobotNatalNet/www";
		scriptDirectory = Environment.getExternalStorageDirectory()
				+ "/sl4a/scripts";

		messages = ((EditText) findViewById(R.id.messageText));
		YesButton = ((Button) findViewById(R.id.YesButton));
		NoButton = ((Button) findViewById(R.id.NoButton));
		statusText = (TextView) findViewById(R.id.textView1);
		statusText.setText("");

		// Não deixa sistema dormir
		pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "RoboDTMF");
		wl.acquire();

		messages.setTextSize(20);
		messages.append("Starting...\r\n\r\n");

		messages.append("Answer Yes or No above:\r\n\r\n");
		messages.append("Do you want to download HTML and additional packages needed for AnWide?\r\n");

		buttonsFunction = BT_DOWNLOAD;
		buttonResult = -1;
		YesButton.setOnClickListener(buttonHandler);
		NoButton.setOnClickListener(buttonHandler);

		YesButton.setVisibility(View.VISIBLE);
		NoButton.setVisibility(View.VISIBLE);

		// init();
	}

	void createHtmlFiles() {
		FileWriter outFile;
		try {
			java.io.File file = new java.io.File(webDirectory, "index.html");
			if (!file.exists()) {

				outFile = new FileWriter(webDirectory + "/index.html");
				PrintWriter out = new PrintWriter(outFile);
				out.println("<html>\n " + "<frameset cols=\"25%,75%\"> \n"
						+ "<frame src=\"image.html\" /> \n 	"
						+ "<frame src=\"commands.html\" /> \n"
						+ "</frameset> \n" + "</html> \n");
				out.close();
			}

			file = new java.io.File(webDirectory, "image.html");
			if (!file.exists()) {

				outFile = new FileWriter(webDirectory + "/image.html");
				PrintWriter out = new PrintWriter(outFile);
				out.println("<html><head>\n "
						+ "<meta http-equiv=\"refresh\" content=\"1;url=/image.html\" /> \n"
						+ "<title>CellBot</title> \n 	" + "</head> \n"
						+ "<img src=CellBotImg.jpg> \n" + "<br> \n"
						+ "--SENSORES--" + "</center>" + "</html>");
				out.close();
			}

			file = new java.io.File(webDirectory, "processos.html");
			if (!file.exists()) {

				outFile = new FileWriter(webDirectory + "/processos.html");
				PrintWriter out = new PrintWriter(outFile);
				out.println("<html>\n "
						+ "Lista de processos em execucao <a href=/processos.html>(atualizar)</a>: \n"
						+ "<br> \n 	" + "<table border=1> \n"
						+ "<tr><td>PID</td><td>Name</td><td>Acao</td></tr> \n"
						+ "PLIST \n" + "</table>" + "<br>" + "</html>");
				out.close();
			}

			file = new java.io.File(webDirectory, "commands.html");
			if (!file.exists()) {

				outFile = new FileWriter(webDirectory + "/commands.html");
				PrintWriter out = new PrintWriter(outFile);
				out.println("<html>Comandos:<BR>\n "
						+ "<a href=commands.html?R>Re</a> | <a href=commands.html?E>Esquerda</a>  \n"
						+ "<br><br>\n 	"
						+ "<a href=/processos.html>Listar processos</a> \n"
						+ "<br><br> \n"
						+ "Programa: \n"
						+ "<form method=\"POST\" action=\"commands.html\">"
						+ "<textarea name=\"program\" cols=\"100\" rows=\"25\">"
						+ "PRGLINES"
						+ "</textarea><br>"
						+ "<input type=\"submit\" name=Executar value=\"Executar\" />"
						+ "<input type=text name=filename value=\"SCRIPT\">"
						+ "<input type=\"submit\" name=Salvar value=\"Salvar\" />"
						+ "<br>"
						+ "</form>"
						+

						"<form>"
						+ "<input type=hidden name=scriptname value=SCRIPT>"
						+ "Script: SCRIPT <input type=\"submit\" name=ASL4 value=\"Executar ASL4\" />"
						+ "</form>" + "<BR><BR>" + "Ultimo comando:" + "ULTIMO"
						+ "<BR><BR>Carregar programas salvos:<BR>" + "PROGRAMA"
						+ "<BR>" + "<br>" + "</html>");
				out.close();
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getBaseContext(),
					"Could not create default HTML pages", Toast.LENGTH_LONG)
					.show();
			ShowErrorAndExit("Could not create default HTML pages");
		}

	}

	void createScriptFiles() {
		FileWriter outFile;
		try {
			outFile = new FileWriter(scriptDirectory + "/slsh");
			PrintWriter out = new PrintWriter(outFile);
			out.println("#! /system/bin/sh\r\n "
					+ "#Automatically generated by NatalNet app\n"
					+ "[ -z \"$1\" ] && exec echo \"please specify the name of a script to run\"\n	"
					+ "am start -a com.googlecode.android_scripting.action.LAUNCH_BACKGROUND_SCRIPT  -n com.googlecode.android_scripting/.activity.ScriptingLayerServiceLauncher  -e com.googlecode.android_scripting.extra.SCRIPT_PATH /sdcard/sl4a/scripts/${1}\n");
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getBaseContext(),
					"Could not create slsh wrapper script", Toast.LENGTH_LONG)
					.show();
		}

		try {
			outFile = new FileWriter(scriptDirectory + "/listsl");
			PrintWriter out = new PrintWriter(outFile);
			out.println("#! /system/bin/sh \n"
					+ "#Automatically generated by NatalNet app\n"
					+ "cd /proc \n"
					+ "for i in 0* 1* 2* 3* 4* 5* 6* 7* 8* 9*;\n" + "do\n"
					+ "R=`grep sl4a $i/cmdline 2> /dev/null` \n" +

					"if [ \"$?\" == \"0\" ];\n" + "then\n" + "echo -n $i\n"
					+ "echo -n \",\" \n" + "echo $R \n" + "fi \n" + "done \n");

			out.close();

			String cmdLine = "chmod +x " + scriptDirectory + "/slsh";
			Runtime.getRuntime().exec(cmdLine);

			cmdLine = "chmod +x " + scriptDirectory + "/listsl";
			Runtime.getRuntime().exec(cmdLine);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Toast.makeText(getBaseContext(),
					"Could not create listsl wrapper script", Toast.LENGTH_LONG)
					.show();
		}

	}

	void createDirectories() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			File file = new File(Environment.getExternalStorageDirectory(),
					"/WebRobotNatalNet/www");
			if (!file.exists()) {
				if (!file.mkdirs()) {
					Toast.makeText(getBaseContext(),
							"Could not create webDirectory", Toast.LENGTH_LONG)
							.show();
				}
			}

			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
		if (!((mExternalStorageAvailable) && (mExternalStorageWriteable))) {
			Toast.makeText(getBaseContext(), "SD card not present",
					Toast.LENGTH_LONG).show();

		}

	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {

		// Salva imagem

		try {
			boolean cam = false;
			if (cam) {
				mCamera.setOneShotPreviewCallback(this);
				Camera.Parameters parameters = camera.getParameters();
				Size size = parameters.getPreviewSize();
				YuvImage image = new YuvImage(data,
						parameters.getPreviewFormat(), size.width, size.height,
						null);

				available.acquire();

				File file = new File(webDirectory + "/CellBotImg.jpg");

				FileOutputStream filecon = new FileOutputStream(file);
				image.compressToJpeg(
						new Rect(0, 0, image.getWidth(), image.getHeight()),
						90, filecon);

				filecon.flush();
				available.release();
			}
		} catch (FileNotFoundException e) {
			Toast toast = Toast
					.makeText(getBaseContext(), e.getMessage(), 1000);
			toast.show();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mCamera.startPreview();
		mCamera.setOneShotPreviewCallback(WebRobot.this);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mCamera = Camera.open();
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			mCamera.release();
		}
		;
		parameters = mCamera.getParameters();
		parameters.setPreviewSize(320, 240);
		parameters.setWhiteBalance(parameters.WHITE_BALANCE_AUTO);
		parameters.setColorEffect(parameters.EFFECT_NONE);
		parameters.setExposureCompensation(0);
		parameters.setZoom(0);
		mCamera.setParameters(parameters);
		mCamera.setDisplayOrientation(90);

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mCamera.setPreviewCallback(null);
		mCamera.stopPreview();
		mCamera.release();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		// case R.id.button1:
		// break;

		}
	}

	class httpRequestHandler implements Runnable {
		final static String CRLF = "\r\n";

		boolean cmdASL4 = false;
		Socket socket;
		InputStream input;
		OutputStream output;
		BufferedReader br;
		String fileNameP = "";

		String command;
		boolean isCommand;
		boolean cmdSalvar = false;
		boolean cmdExecutar = false;

		public httpRequestHandler(Socket socket) throws Exception {
			this.socket = socket;
			this.input = socket.getInputStream();
			this.output = socket.getOutputStream();
			this.br = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));
		}

		public void run() {
			try {
				processRequest();
			} catch (Exception e) {
				System.out.println(e);
			}
		}

		private String URLdecode(String in) {
			String out = "";
			try {
				out = URLDecoder.decode(in, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return out;
		}

		private void processRequest() throws Exception {
			while (true) {

				String headerLine = br.readLine();
				System.out.println(headerLine);
				if (headerLine.equals(CRLF) || headerLine.equals(""))
					break;

				StringTokenizer s = new StringTokenizer(headerLine);
				String temp = s.nextToken();

				if (temp.equals("POST")) {
					// anwide.cgi
					String cgiName = s.nextToken();
					System.out.println("CGI = " + cgiName);
					String POST_Request = "";

					do {
						POST_Request += (char) br.read();
					} while (br.ready());
					System.out.println("POST REQUEST RAW:\r\n" + POST_Request);

					// Prepare response
					String serverLine = "Server: AnWide";
					String statusLine = "HTTP/1.0 200 OK" + CRLF;
					String contentTypeLine = "text/html";
					String entityBody = null;
					String contentLengthLine = "";																
						entityBody = "<HTML>"								
								+ "<BODY>Program execution started!<br><pre>" //+ 		POST_Request						
								+ "</BODY></HTML>";
						contentLengthLine = "Content-Length: "
								+ entityBody.length() + CRLF;
										
					// End prepare response

					if (cgiName.contentEquals("/anwide.cgi")) {
						System.out.println("Executar programa");
						
						//Empty the log file
						FileWriter outLog;
						try {
							java.io.File file = new java.io.File("/mnt/sdcard", "/myfile.log");
							if (!file.exists()) {

								outLog = new FileWriter(webDirectory + "/index.html");
								PrintWriter out = new PrintWriter(outLog);
								out.println("");
								out.close();
							}
						} catch (Exception e) {};
						
						
						if (POST_Request.indexOf("value1=") > 1) {
							String rawProgram = POST_Request.substring(POST_Request
									.indexOf("value1="));
							String decodedProgram = URLdecode(rawProgram)
									.substring(8).replaceAll("&Salvar=Salvar", "");
							
							// &filename=filename
							String filename = "tmp.py";

							System.out.println("POST REQUEST decoded:\r\n"
									+ decodedProgram);
							
							
							//Add calls to redirect the programs stderr and stdout to a file that we will show to the user in the browser 
							String finalProgram = "" +
									"#" + CRLF +
									"#Automcally generated by AnWide" + CRLF +
									"import sys" + CRLF +
									"import android" + CRLF +
									"from datetime import datetime" + CRLF +
									"droid = android.Android()" + CRLF +
									CRLF +
									"f = open(\"/mnt/sdcard/myfile.log\", \"w\", 0)" + CRLF +
									"sys.stderr = f" + CRLF +
									"sys.stdout = f" + CRLF +
									CRLF + "#User program start:" + CRLF +
									CRLF + "print \"Program started at \" + str(datetime.now())" + CRLF +
									decodedProgram +
									CRLF + "#User program end" + CRLF +
									CRLF + "print \"Program finished at \" + str(datetime.now())" + CRLF +
									"f.close()" + CRLF + CRLF +
									"#Program end";
									 									 

							System.out.println("GRAVANDO " + filename);
							FileWriter outFile = new FileWriter(scriptDirectory
									+ "/" + filename);
							PrintWriter out = new PrintWriter(outFile);
							out.println(finalProgram);
							out.close();
							System.out.println(filename + " GRAVADO!");
							

							Process process;
							String cmdLine = "/system/bin/sh "
									+ scriptDirectory + "/slsh " + filename;
							process = Runtime.getRuntime().exec(cmdLine);
							// BufferedReader in = new BufferedReader(new
							// InputStreamReader(process.getInputStream()));
							
						}
					} else
					if (cgiName.contentEquals("/output.cgi")) {
						entityBody="<pre>";
						//Try to open the file with the stderr and stdout
						try {
							FileInputStream fstream = new FileInputStream(
									"/mnt/sdcard/myfile.log");
							DataInputStream in = new DataInputStream(
									fstream);
							BufferedReader br = new BufferedReader(
									new InputStreamReader(in));
							String strLine;
							while ((strLine = br.readLine()) != null) {
								// System.out.println (strLine);
								// program+=strLine.replaceAll(";",
								// ";\r\n");
								entityBody += strLine + "\r\n";
							}
							// Close the input stream
							in.close();
						} catch (Exception e) {// Catch exception if any
							System.err.println("Error: "
									+ e.getMessage());
						}
						entityBody+="</pre>";
						
					}

					else {

						String rawProgram = POST_Request.substring(POST_Request
								.indexOf("program="));

						String decodedProgram = URLdecode(rawProgram)
								.substring(8).replaceAll("&Salvar=Salvar", "");

						// &filename=filename
						String filename = decodedProgram
								.substring(decodedProgram.lastIndexOf("=") + 1);

						System.out.println("FILENAME ===== " + filename);

						decodedProgram = decodedProgram.substring(0,
								decodedProgram.lastIndexOf("&"));

						System.out.println("POST REQUEST decoded:\r\n"
								+ decodedProgram);

						System.out.println("GRAVANDO " + filename);
						FileWriter outFile = new FileWriter(scriptDirectory
								+ "/" + filename);
						PrintWriter out = new PrintWriter(outFile);
						out.println(decodedProgram);
						out.close();
						System.out.println(filename + " GRAVADO!");

						temp = "GET";
					}
					
					//Send response
					// Send the status line.
					output.write(statusLine.getBytes());
					System.out.println(statusLine);

					// Send the server line.
					output.write(serverLine.getBytes());
					System.out.println(serverLine);

					// Send the content type line.
					output.write(contentTypeLine.getBytes());
					System.out.println(contentTypeLine);
					
					// Send the Content-Length
					output.write(contentLengthLine.getBytes());
					System.out.println(contentLengthLine);

					// Send a blank line to indicate the end of the header
					// lines.
					output.write(CRLF.getBytes());
					System.out.println(CRLF);
					System.out.println(entityBody);
					output.write(entityBody.getBytes());
					System.out.println(CRLF);
					output.close();
				}

				boolean fileExists = false;
				FileInputStream fis = null;
				if (temp.equals("GET")) {

					String fileName = s.nextToken();

					System.out.println("SIZE = " + fileName.length());
					System.out.println("FNAME = " + fileName);
					if (fileName.length() <= 1) {
						fileName = "/index.html";
					}

					// Trata requisicao de paginas HTML estáticas
					if (fileName.endsWith("html") || fileName.endsWith("htm")
							|| fileName.endsWith("js")
							|| fileName.endsWith("wav")
							|| fileName.endsWith("png")
							|| fileName.endsWith("css")
							|| fileName.endsWith("jpg")
							|| fileName.endsWith("gif")
							|| fileName.endsWith("cur")) {
						fileNameP = webDirectory + fileName;
						System.out.println("Name: " + fileNameP);
						System.out.println("Tentando abrir...");
						try {
							fis = new FileInputStream(fileNameP);
							fileExists = true;
							System.out.println("abriu");
						} catch (FileNotFoundException e) {
							fileExists = false;
							System.out.println("nao abriu");
						}
						System.out.println("acho que foi");

					} else {

						command = "";
						isCommand = false;

						boolean sensors = false;
						boolean plist = false;

						if (fileName.startsWith("/image.html")) {
							sensors = true;
							isCommand = true;
						}

						if (fileName.startsWith("/processos.html")) {
							plist = true;
							isCommand = true;
						}

						if (fileName.startsWith("/commands.html")) {
							if (fileName.length() > 14) {
								command = fileName.substring(14);
								command = command.replaceAll("%3A", ":");
								command = command.replaceAll("%3B", ";");
								command = command.replaceAll("%0D%0A", " ");
							} else
								command = "Nenhum comando";
							fileName = "/commands.html";
							System.out.println("Comando recebido!");
							isCommand = true;

							if (command.contains("&Salvar=Salvar")) {
								cmdSalvar = true;
								command.replaceAll("&Salvar=Salvar", "");
							}
							if (command.contains("&Executar=Executar")) {
								cmdExecutar = true;
								command.replaceAll("&Executar=Executar", "");
							}

							if (command.contains("&ASL4=Executar+ASL4")) {
								cmdASL4 = true;
								command = command.replaceAll(
										"&ASL4=Executar+ASL4", "");
							}
						}

						// Lista de programas armazenados em /mnt/sdcard/PRG/
						String programList = "";

						if (isCommand) {
							// File folder = new File("/mnt/sdcard/PRG/");
							File folder = new File(scriptDirectory);

							File[] listOfFiles = folder.listFiles();

							for (int i = 0; i < listOfFiles.length; i++) {
								if (listOfFiles[i].isFile()) {
									// System.out.println("File " +
									// listOfFiles[i].getName());
									programList += "<a href=commands.html?carregar="
											+ listOfFiles[i].getName()
											+ ">"
											+ listOfFiles[i].getName()
											+ "</a> <BR>";
								} // else if (listOfFiles[i].isDirectory()) {
									// System.out.println("Directory " +
									// listOfFiles[i].getName());
									// }
							}

							fileName = webDirectory + fileName;
							System.out.println("Name: " + fileName);
						}

						// Conteudo do programa
						String program = "Entre com seu programa ou clique em um pronto abaixo para carregar";
						if (isCommand) {
							fileExists = true;
							if (command.startsWith("?carregar=")) {
								// program="carregar...";
								StringTokenizer st1 = new StringTokenizer(
										command, "=");
								String t = st1.nextToken();
								t = st1.nextToken();
								lastScript = t;

								program = "";

								try {
									FileInputStream fstream = new FileInputStream(
											scriptDirectory + "/" + t);
									DataInputStream in = new DataInputStream(
											fstream);
									BufferedReader br = new BufferedReader(
											new InputStreamReader(in));
									String strLine;
									while ((strLine = br.readLine()) != null) {
										// System.out.println (strLine);
										// program+=strLine.replaceAll(";",
										// ";\r\n");
										program += strLine + "\r\n";
									}
									// Close the input stream
									in.close();
								} catch (Exception e) {// Catch exception if any
									System.err.println("Error: "
											+ e.getMessage());
								}
							}

							if (isCommand & cmdSalvar) {
								try {
									String filename = "tmp.txt";
									StringTokenizer st1 = new StringTokenizer(
											command, "=");
									String t = st1.nextToken();
									t = st1.nextToken();
									filename = st1.nextToken().replaceAll(
											"&Salvar", "");

									// System.out.println("GRAVAR FNAME ============ "
									// + filename);

									FileWriter outFile = new FileWriter(
											scriptDirectory + filename);
									PrintWriter out = new PrintWriter(outFile);

									String contents = command.replaceAll(
											"\\?program=", "").replaceAll(
											"&Salvar=Salvar", "");
									contents = contents
											.replaceAll(";", ";\r\n");

									st1 = new StringTokenizer(contents, "&");
									contents = st1.nextToken();

									out.println(contents);
									// out.println("This is line 2");
									// out.print("This is line3 part 1, ");
									// out.println("this is line 3 part 2");
									out.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

						// FileInputStream fis = null;

						available.acquire();
						try {
							if (isCommand) {

								BufferedReader reader = new BufferedReader(
										new FileReader(fileName));
								PrintWriter writer = new PrintWriter(
										new FileWriter(fileName + ".tmp"));
								String line = null;
								while ((line = reader.readLine()) != null) {
									if (plist) {
										// System.out.println("PLISSSSSSST = " +
										// line.indexOf("PLIST"));
										if (line.indexOf("PLIST") >= 0) {
											System.out
													.println("VAMOS LISTAR OS PROCESSOS!");
											Process process;
											String cmdLine = "/system/bin/sh "
													+ scriptDirectory
													+ "/listsl";
											process = Runtime.getRuntime()
													.exec(cmdLine);
											BufferedReader inp = new BufferedReader(
													new InputStreamReader(
															process.getInputStream()));
											String pline = "", str = "";
											while ((pline = inp.readLine()) != null) {
												StringTokenizer st1 = new StringTokenizer(
														pline, ",");
												String pid = st1.nextToken();
												String name = st1.nextToken();
												str += "<tr><td>"
														+ pid
														+ "</td><td>"
														+ name
														+ "</td><td><a href=>Kill</a></td></tr>";
											}
											// Close the input stream
											inp.close();
											writer.println(line.replaceAll(
													"PLIST", str));
										} else
											writer.println(line);

									} else if (sensors) {

										String sensorData = "";
										try {
											FileInputStream fstream = new FileInputStream(
													webDirectory
															+ "/sensors.html");
											DataInputStream in = new DataInputStream(
													fstream);
											BufferedReader br = new BufferedReader(
													new InputStreamReader(in));
											String strLine;
											while ((strLine = br.readLine()) != null) {
												sensorData += strLine + "\r\n";
											}
											// Close the input stream
											in.close();
										} catch (Exception e) {// Catch
																// exception if
																// any
											System.err.println("Error: "
													+ e.getMessage());
										}
										writer.println(line.replaceAll(
												"--SENSORES--", sensorData));
									} else {

										writer.println(line
												.replaceAll("ULTIMO", command)
												.replaceAll("PROGRAMA",
														programList)
												.replaceAll("PRGLINES", program)
												.replaceAll("SCRIPT",
														lastScript));
									}

								}

								reader.close();
								writer.close();

								fis = new FileInputStream(fileName + ".tmp");
							} else
								fis = new FileInputStream(fileName);
							fileExists = true;

						} catch (FileNotFoundException e) {
							fileExists = false;
						}
					}

					System.out.println("A");
					String serverLine = "Server: Simple Java Http Server";
					String statusLine = null;
					String contentTypeLine = null;
					String entityBody = null;
					String contentLengthLine = "error";
					if (fileExists) {
						System.out.println("B");
						statusLine = "HTTP/1.0 200 OK" + CRLF;
						System.out.println("B1");
						contentTypeLine = "Content-type: "
								+ contentType(fileName) + CRLF;
						System.out.println("B2");
						contentLengthLine = "Content-Length: "
								+ (new Integer(fis.available())).toString()
								+ CRLF;
						System.out.println("B3");

					} else {
						System.out.println("C");
						statusLine = "HTTP/1.0 404 Not Found" + CRLF;
						contentTypeLine = "text/html";
						entityBody = "<HTML>"
								+ "<HEAD><TITLE>404 Not Found</TITLE></HEAD>"
								+ "<BODY>404 Not Found"
								+ "<br>usage:http://yourHostName:port/"
								+ "fileName.html</BODY></HTML>";
						contentLengthLine = "Content-Length: "
								+ entityBody.length() + CRLF;
					}

					System.out.println("F");

					System.out.println("2");
					// Send the status line.
					output.write(statusLine.getBytes());
					System.out.println(statusLine);

					System.out.println("3");
					// Send the server line.
					output.write(serverLine.getBytes());
					System.out.println(serverLine);

					System.out.println("4");
					// Send the content type line.
					output.write(contentTypeLine.getBytes());
					System.out.println(contentTypeLine);

					System.out.println("5");
					// Send the Content-Length
					output.write(contentLengthLine.getBytes());
					System.out.println(contentLengthLine);

					// Send a blank line to indicate the end of the header
					// lines.
					output.write(CRLF.getBytes());
					System.out.println(CRLF);

					// Send the entity body.
					if (fileExists) {
						sendBytes(fis, output);
						fis.close();
						available.release();
					} else {
						output.write(entityBody.getBytes());
					}

					if (isCommand && cmdASL4) {

						if (command.startsWith("?scriptname=")) {
							StringTokenizer st1 = new StringTokenizer(command,
									"=");
							String t = st1.nextToken();
							t = st1.nextToken();
							st1 = new StringTokenizer(t, "&");
							t = st1.nextToken();
							lastScript = t;

							// System.out.println(" ===================================================="
							// + lastScript);

							Process process;
							String cmdLine = "/system/bin/sh "
									+ scriptDirectory + "/slsh " + lastScript;
							process = Runtime.getRuntime().exec(cmdLine);
							// BufferedReader in = new BufferedReader(new
							// InputStreamReader(process.getInputStream()));

						}

					}

					if (isCommand && cmdExecutar) {

						if (command.startsWith("?F")) {
							System.out.println(" <<<<<<<< FRENTE >>>>>>>>>>>>");
							// Comando para robo ir para frente...

						}
						if (command.startsWith("?R")) {
							System.out.println(" <<<<<<<< REH >>>>>>>>>>>>");
						}
						if (command.startsWith("?E")) {
							System.out
									.println(" <<<<<<<< ESQUERDA >>>>>>>>>>>>");

						}
						if (command.startsWith("?D")) {
							System.out
									.println(" <<<<<<<< DIREITA >>>>>>>>>>>>");

							Thread.sleep(600);

						}
						if (command.startsWith("?B")) {
							System.out.println(" <<<<<<<< BOLA >>>>>>>>>>>>");
						}
						if (command.startsWith("?L")) {
							System.out.println(" <<<<<<<< LINHA >>>>>>>>>>>>");
						}
						if (command.startsWith("?program")) {
							System.out
									.println(" <<<<<<<< PROGRAMA >>>>>>>>>>>>");

							// Interpreta comandos
							String commandList = command.substring(9);
							System.out.println("Command list = " + commandList);
							StringTokenizer st1 = new StringTokenizer(
									commandList, "; ");

							// Ordena de acordo com os numeros
							// while (st1.hasMoreTokens()) {

							// }

							// Executa
							while (st1.hasMoreTokens()) {
								String cmd = st1.nextToken();
								StringTokenizer st2 = new StringTokenizer(cmd,
										":");
								String param = st2.nextToken();
								param = st2.nextToken();

								System.out.println("Executando: " + cmd
										+ " com parametro " + param);
								if (cmd.contains("FRENTE:"))
									System.out.println("IR PARA FRENTE");
								if (cmd.contains("DIREITA:"))
									System.out.println("IR PARA Direita...");

								if (cmd.contains("PARE:"))
									System.out.println("Full stop...");

							} //
								//
						}

					}

				}
			}

			try {
				output.close();
				br.close();
				socket.close();
			} catch (Exception e) {
			}
		}

		private void sendBytes(FileInputStream fis, OutputStream os)
				throws Exception {

			byte[] buffer = new byte[1024];
			int bytes = 0;

			while ((bytes = fis.read(buffer)) != -1) {
				os.write(buffer, 0, bytes);
			}
		}

		private String contentType(String fileName) {
			if (fileName.endsWith(".htm") || fileName.endsWith(".html")
					|| fileName.endsWith(".txt")) {
				return "text/html";
			} else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
				return "image/jpeg";
			} else if (fileName.endsWith(".gif")) {
				return "image/gif";
			} else {
				return "application/octet-stream";
			}
		}
	}

	// this is our download file asynctask
	class DownloadFileAsync extends AsyncTask<String, String, String> {
		ProgressBar pg = (ProgressBar) findViewById(R.id.progressBar1);
		TextView st = (TextView) findViewById(R.id.textView1);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pg.setMax(100);
			pg.setProgress(0);
		}

		@Override
		protected String doInBackground(String... aurl) {

			try {

				// connecting to url
				URL u = new URL(fileURL);
				HttpURLConnection c = (HttpURLConnection) u.openConnection();
				c.setRequestMethod("GET");
				c.setDoOutput(true);
				c.connect();

				// lenghtOfFile is used for calculating download progress
				int lenghtOfFile = c.getContentLength();

				// this is where the file will be seen after the download
				FileOutputStream f = new FileOutputStream(new File(webDirectory
						+ "/", fileName));
				// file input is from the url
				InputStream in = c.getInputStream();

				// here's the download code
				byte[] buffer = new byte[1024];
				int len1 = 0;
				long total = 0;

				while ((len1 = in.read(buffer)) > 0) {
					total += len1; // total = total + len1
					publishProgress("" + (int) ((total * 100) / lenghtOfFile));
					// st.setText(total + " of " + lenghtOfFile + " bytes ("
					// +(int) ((total * 100) / lenghtOfFile) + "%)" );
					f.write(buffer, 0, len1);

				}
				f.close();
				downloadDone = true;

			} catch (Exception e) {
				Log.d("WebRobotNatalNet", e.getMessage());
			}

			return null;
		}

		protected void onProgressUpdate(String... progress) {

			pg.setProgress(Integer.parseInt(progress[0]));
			st.setText(progress[0] + "%");

		}

		@Override
		protected void onPostExecute(String unused) {

			if (!downloadDone) {
				System.out.println("DOWNLOAD ERROR");
				AlertDialog.Builder dlgAlert = new AlertDialog.Builder(
						WebRobot.this);
				dlgAlert.setMessage(
						"File could not be downloaded from internet.")
						.setIcon(android.R.drawable.ic_dialog_alert)
						.setTitle("Download error")
						.setPositiveButton("Quit",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int which) {
										// dismiss the dialog
										finish();
									}
								}).show();
			} else {

				System.out.println("===> Unzip FILES ==== ");
				String zipFile = webDirectory + "/files.zip";
				String unzipLocation = webDirectory + "/";
				messages.setText("\r\n Uncompressing files...\r\n(see progress on bar above)");
				buttonResult = 3;
				new UnzipFilesAsync().execute(zipFile, unzipLocation);

			}
		}
	}

	class UnzipFilesAsync extends AsyncTask<String, String, String> {
		String unzipLocation = webDirectory + "/";

		ProgressBar pg = (ProgressBar) findViewById(R.id.progressBar1);
		TextView st2 = (TextView) findViewById(R.id.textView1);

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pg.setMax(100);
			pg.setProgress(0);
		}

		@Override
		protected String doInBackground(String... aurl) {

			try {
				_dirChecker("");

				String zipFile = webDirectory + "/files.zip";
				FileInputStream fin = new FileInputStream(zipFile);
				ZipInputStream zin = new ZipInputStream(fin);
				ZipEntry ze = null;
				int numFiles = 0;
				while ((ze = zin.getNextEntry()) != null) {
					numFiles++;
				}
				System.out.println("Zipped files = " + numFiles);

				fin = new FileInputStream(zipFile);
				zin = new ZipInputStream(fin);
				zin = new ZipInputStream(fin);
				ze = null;

				int total = 0;
				while ((ze = zin.getNextEntry()) != null) {
					Log.v("Decompress", "Async Unzipping " + ze.getName());

					if (ze.isDirectory()) {
						_dirChecker(ze.getName());
					} else {
						FileOutputStream fout = new FileOutputStream(
								unzipLocation + ze.getName());
						for (int c = zin.read(); c != -1; c = zin.read()) {
							fout.write(c);
						}

						zin.closeEntry();
						fout.close();
					}

					total++;
					publishProgress("" + (int) ((total * 100) / numFiles));
					// st2.setText(total + " of " + numFiles + " files (" +(int)
					// ((total * 100) / numFiles) + "%)" );

				}
				zin.close();
			} catch (Exception e) {
				Log.e("Decompress", "unzip", e);
			}

			return null;
		}

		private void _dirChecker(String dir) {
			File f = new File(unzipLocation + dir);

			if (!f.isDirectory()) {
				f.mkdirs();
			}
		}

		protected void onProgressUpdate(String... progress) {

			pg.setProgress(Integer.parseInt(progress[0]));
			st2.setText(progress[0] + "%");
		}

		@Override
		protected void onPostExecute(String unused) {

			// Aqui terminou de descompactar os arquivos
			System.out.println("UNZIP ASYNC ENDED!");
			buttonResult = 4;
			checkDependencies();

		}
	}

	/**
	 * 
	 * @author jon
	 */
	public class Decompress {
		private String _zipFile;
		private String _location;
		private ProgressDialog progressBar;
		private int progressBarStatus;
		private int fileSize;

		public Decompress(String zipFile, String location) {
			_zipFile = zipFile;
			_location = location;

			_dirChecker("");
		}

		public void unzip() {
			try {
				FileInputStream fin = new FileInputStream(_zipFile);
				ZipInputStream zin = new ZipInputStream(fin);
				ZipEntry ze = null;
				int numFiles = 0;
				while ((ze = zin.getNextEntry()) != null) {
					numFiles++;
				}
				System.out.println("Zipped files = " + numFiles);

				fin = new FileInputStream(_zipFile);
				zin = new ZipInputStream(fin);
				zin = new ZipInputStream(fin);
				ze = null;

				while ((ze = zin.getNextEntry()) != null) {
					Log.v("Decompress", "Unzipping " + ze.getName());

					if (ze.isDirectory()) {
						_dirChecker(ze.getName());
					} else {
						FileOutputStream fout = new FileOutputStream(_location
								+ ze.getName());
						for (int c = zin.read(); c != -1; c = zin.read()) {
							fout.write(c);
						}

						zin.closeEntry();
						fout.close();
					}

				}
				zin.close();
			} catch (Exception e) {
				Log.e("Decompress", "unzip", e);
			}

		}

		private void _dirChecker(String dir) {
			File f = new File(_location + dir);

			if (!f.isDirectory()) {
				f.mkdirs();
			}
		}
	}

	private boolean appInstalledOrNot(String uri) {
		PackageManager pm = getPackageManager();
		boolean app_installed = false;
		try {
			PackageInfo s = pm.getPackageInfo(uri,
					PackageManager.GET_ACTIVITIES);
			System.out.println(s.packageName.toString());
			System.out.println(s.applicationInfo.toString());
			app_installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			app_installed = false;
		}
		return app_installed;
	}

	public void ShowErrorAndExit(String msg) {
		AlertDialog alertDialog = new AlertDialog.Builder(this).create();
		alertDialog.setTitle("Error");
		alertDialog.setMessage(msg);
		alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				// here you can add functions
				finish();
			}
		});
		alertDialog.setIcon(R.drawable.icon);
		alertDialog.show();
	}

	public void checkDependencies() {
		Button yes = ((Button) findViewById(R.id.YesButton));
		Button no = ((Button) findViewById(R.id.NoButton));
		TextView stat = (TextView) findViewById(R.id.textView1);

		yes.setEnabled(false);
		no.setEnabled(false);

		System.out.println("==== checkDependencies() ====");
		EditText msg = ((EditText) findViewById(R.id.messageText));
		msg.setText("Checking dependencies...\r\n");

		// Verifica se SL4A5 esta instalado
		boolean SL4AInstalled = appInstalledOrNot("com.googlecode.android_scripting");
		if (SL4AInstalled) {
			System.out.println("Android scripting found");
			msg.setText("Checking dependencies...\r\n");
			msg.append("Android scripting found\r\n");
			init();

			// Verifica se o Python esta instlado
			boolean PythonInstalled = appInstalledOrNot("com.googlecode.pythonforandroid");
			if (PythonInstalled) {
				System.out.println("Python for Android found");
				msg.append("Python for Android found\r\n");
			} else {

				System.out.println("Python for Android not installed");
				msg.setText("Python for Android NOT found. Install? (After installing the App, open it and click Install)\r\n");
				yes.setEnabled(true);
				no.setEnabled(true);
				buttonsFunction = BT_PYTHON;

			}

		} else {
			System.out.println("Android Scripting is not installed");
			msg.append("ERROR: Android scripting NOT found. Install?\r\n");

			yes.setVisibility(View.VISIBLE);
			no.setVisibility(View.VISIBLE);
			yes.setEnabled(true);
			no.setEnabled(true);

			buttonsFunction = BT_CHECK;
		}
	}

	void setup() {

		createDirectories();

		// checkDependencies();
	}

	View.OnClickListener buttonHandler = new View.OnClickListener() {
		public void onClick(View v) {

			if (YesButton.getId() == ((Button) v).getId()) {
				if (buttonsFunction == BT_DOWNLOAD) {
					messages.setText("\r\n Downloading files...\r\n(see progress on bar above)");
					YesButton.setEnabled(false);
					NoButton.setEnabled(false);
					new DownloadFileAsync().execute(fileURL);
				} else if (buttonsFunction == BT_CHECK) {

					// Check if file exists
					System.out.println("Checking for sl4a_r5.apk");
					java.io.File file = new java.io.File(webDirectory,
							"/sl4a_r5.apk");
					if (!file.exists()) {
						System.out.println("NOT FOUND");
						AlertDialog.Builder dlgAlert = new AlertDialog.Builder(
								WebRobot.this);
						dlgAlert.setMessage(
								"sl4a_r5.apk not found. Try to reopen and download HTML files")
								.setIcon(android.R.drawable.ic_dialog_alert)
								.setTitle("Error")
								.setPositiveButton("Quit",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												// dismiss the
												// dialog
												finish();
											}
										}).show();

					} else {

						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(
								Uri.fromFile(new File(webDirectory
										+ "/sl4a_r5.apk")),
								"application/vnd.android.package-archive");
						startActivity(intent);
					}

					// Verifica se o Python esta instlado
					boolean PythonInstalled = appInstalledOrNot("com.googlecode.pythonforandroid");
					if (PythonInstalled) {
						System.out.println("Python for Android found");
						messages.append("Python for Android found\r\n");
					} else {

						System.out.println("Python for Android not installed");
						messages.append("Python for Android NOT found. Install? (After installing the App, open it and click Install)\r\n");
						buttonsFunction = BT_PYTHON;

					}

				} else if (buttonsFunction == BT_PYTHON) {

					// Check if file exists
					// PythonForAndroid_r4
					System.out.println("Checking for PythonForAndroid_r4.apk");
					messages.append("\r\nInstalling Python...");
					java.io.File file = new java.io.File(webDirectory,
							"/PythonForAndroid_r4.apk");
					if (!file.exists()) {
						System.out.println("NOT FOUND");
						AlertDialog.Builder dlgAlert = new AlertDialog.Builder(
								WebRobot.this);
						dlgAlert.setMessage(
								"PythonForAndroid_r4.apk not found. Try to reopen and download HTML files")
								.setIcon(android.R.drawable.ic_dialog_alert)
								.setTitle("Error")
								.setPositiveButton("Quit",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												// dismiss the
												// dialog
												finish();
											}
										}).show();

					} else {

						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setDataAndType(
								Uri.fromFile(new File(webDirectory
										+ "/PythonForAndroid_r4.apk")),
								"application/vnd.android.package-archive");
						startActivity(intent);
					}

				}

			} else if (NoButton.getId() == ((Button) v).getId()) {
				if (buttonsFunction == BT_DOWNLOAD) {
					buttonResult = 1;
					checkDependencies();

				} else if ((buttonsFunction == BT_CHECK)
						|| (buttonsFunction == BT_PYTHON)) {
					messages.setText("\r\nSorry, you cannot continue without the required packages. Please download them. If you just installed, close and open this program.\r\n");
				}
			}
		}
	};

	void init() {

		messages.append("\r\nCreating files...\r\n");

		createScriptFiles();
		createHtmlFiles();

		messages.append("Starting webserver...\r\n");

		// Servidor web
		new Thread(new Runnable() {
			ServerSocket ss;
			int ton, toff, codeFront, codeBack;
			String params = "6,9,1000,10,2.13";

			public void run() {

				boolean ok = false;
				int porta = 8000;

				while (!ok) {
					try {
						ss = new ServerSocket(porta);
						ok = true;
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
						porta++;
						if (porta > 8020)
							return;
					}
				}

				String ipString = "?";
				try {
					Log.v("DEBUGR", "WM1");
					WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
					Log.v("DEBUGR", "WM2");
					WifiInfo wifiInfo = wifiManager.getConnectionInfo();
					Log.v("DEBUGR", "WM3");
					int ipAddress = wifiInfo.getIpAddress();
					Log.v("DEBUGR", "WM4");

					ipString = String.format("%d.%d.%d.%d", (ipAddress & 0xff),
							(ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff),
							(ipAddress >> 24 & 0xff));

					Log.v("DEBUGR", "WM5");

				} catch (Exception e) {
					Log.v("DEBUGR", "WM6");
					try {
						Socket socket = new Socket("www.claro.com.br", 80);
						Log.i("", socket.getLocalAddress().toString());
					} catch (Exception e2) {
						Log.i("", e.getMessage());
						setTitle("RoboDTMF NatalNet (No Wifi IP)");
					}

				}
				final String hostport = ipString + ":" + porta;
				runOnUiThread(new Runnable() {
					public void run() {
						Window w = getWindow();
						w.setTitle("RoboDTMF NatalNet: " + hostport);
					}
				});

				// server infinite loop
				while (true) {
					Socket socket;
					try {
						socket = ss.accept();

						System.out.println("New connection accepted "
								+ socket.getInetAddress() + ":"
								+ socket.getPort());

						// Construct handler to process the HTTP request
						// message.

						httpRequestHandler request = new httpRequestHandler(
								socket);
						// Create a new thread to process the request.
						Thread thread = new Thread(request);

						// Start the thread.
						thread.start();
					} catch (Exception e) {
						System.out.println(e);
					}
				}

			}

		}).start();

	}
}
