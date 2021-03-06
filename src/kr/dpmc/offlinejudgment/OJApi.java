package kr.dpmc.offlinejudgment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class OJApi {

	/**
	 * 파일 확장자를 추출함
	 * 
	 * @param fileName 파일 이름
	 * @return 파일 확장자 (.미포함)
	 */
	public static String getFileExtension(String fileName) {
		int i1 = fileName.lastIndexOf('.');
		if (i1 == -1) {
			return null;
		} else {
			String extension = fileName.substring(i1 + 1);
			return extension;
		}
	}

	public static Comparator<MetaDataHW> comparatorMeta = new Comparator<MetaDataHW>() {
		@Override
		public int compare(MetaDataHW m1, MetaDataHW m2) {
			int id1 = Integer.valueOf(m1.id);
			int id2 = Integer.valueOf(m2.id);
			if (id1 > id2) {
				return 1;
			} else if (id1 == id2) {
				return 0;
			} else {
				return -1;
			}
		}
	};

	public static Comparator<String> comparatorString = new Comparator<String>() {
		@Override
		public int compare(String arg0, String arg1) {
			return Integer.compare(Integer.valueOf(arg0), Integer.valueOf(arg1));
		}
	};

	public static List<String[]> getStudentList(YamlConfiguration config, int classNumber) {
		List<String[]> list = new LinkedList<>();

		String fileName = String.format("%03d.xls", classNumber);
		File excelFile = new File(config.getString("정규화.출석부폴더"), fileName);
		HSSFWorkbook workBook = null;
		try {
			workBook = new HSSFWorkbook(new FileInputStream(excelFile));
		} catch (IOException e) {
			e.printStackTrace();
		}

		HSSFSheet sheet = workBook.getSheetAt(0);
		int rows = sheet.getPhysicalNumberOfRows();
		for (int i = 1; i < rows; i++) {
			// 학번과 이름 가져오는 기능 함수로 만들어서 OJAPI로 이동
			HSSFRow row = sheet.getRow(i);
			String id = row.getCell(1).getStringCellValue();
			String name = row.getCell(2).getStringCellValue();
			String[] args = new String[] { id, name };
			list.add(args);
		}
		return list;
	}

	/**
	 * 지정 키워드와 주석을 제거하는 함수
	 * 
	 * @param s 문자열 라인 1줄
	 * @param exceptionList 제거할 키워드
	 * @param isRemoveDescription 주석제거 여부
	 * @return 조건에 따라 제거한 문자열
	 */
	public static String stringLineConvertToException(String s, List<String> exceptionList, boolean isRemoveDescription) {
		if (isRemoveDescription) {
			int i1 = s.indexOf('#');
			if (i1 != -1) {
				s = s.substring(0, i1);
			}
		}
		if (exceptionList != null) {
			for (String ex : exceptionList) {
				s = s.replace(ex, "");
			}
		}
		return s;
	}

	public static List<String> getSourceCodeToStringBuilder_UTF16(File file) {
		// 파일 읽어오기 list로
		try {
			List<String> list = new LinkedList<>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-16"));
			String s;
			while ((s = br.readLine()) != null) {
				list.add(s);
			}
			br.close();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getSourceCodeToStringBuilder_UTF8(File file) {
		// 파일 읽어오기 list로
		try {
			List<String> list = new LinkedList<>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			String s;
			while ((s = br.readLine()) != null) {
				list.add(s);
			}
			br.close();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<String> getSourceCodeToStringBuilder(File file) {
		// 파일 읽어오기 list로
		try {
			List<String> list = new LinkedList<>();
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
			String s;
			while ((s = br.readLine()) != null) {
				list.add(s);
			}
			br.close();
			return list;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static int MAXLENGTH = 50;

	public static void printSwagWithAccent(String str) throws Exception {
		printSwag(str, 25, " !!!", "!!!");
	}

	public static void printSwagWithStars(String str, long delay) throws Exception {
		printSwag(str, delay, " ***", "***");
	}

	public static void printSwagWithBraket(String str) throws Exception {
		printSwag(str, 50, " [[[[", "]]]]");
	}

	public static void printSwag(String str, long delay, String start, String end) throws Exception {
		int st = MAXLENGTH/2 - str.length()/2;
		System.out.print(start);
		for (int i = 0; i < st; i++) {
			System.out.print(' ');
		}
		for (int i = 0; i < str.length(); i++) {
			System.out.print(str.charAt(i));
			Thread.sleep(delay/2);
		}
		
		for (int i = 0; i < st; i++) {
			System.out.print(' ');
		}
		System.out.println(end);
		//System.out.println("st=" + st + " size/2=" + str.length()/2);
	}

	public static void printSwag2(String str, long delay, String start, String end) throws Exception {
		int strlen = 0;
		for (int i = 0; i < str.length(); i++) {
			if ('가' <= str.charAt(i) && str.charAt(i) <= '힣') {
				strlen += 2;
			} else {
				strlen += 1;
			}
		}

		// System.out.println(str + "#" + strlen + "#" + str.length());

		System.out.print(start);

		int blank = MAXLENGTH - strlen;
		if (blank < 0) {
			blank = (int) (MAXLENGTH * 1.5 - strlen);
		}
		for (int i = 0; i < blank / 2; i++) {
			System.out.print(' ');
		}
		if (blank % 2 == 0) {
			System.out.print(' ');
		}
		Thread.sleep(delay / 2);

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);
			System.out.print(c);
			if (c == ' ') {
				Thread.sleep(delay / 2);
			} else {
				Thread.sleep(delay);
			}
		}
		for (int i = 0; i < blank / 2; i++) {
			System.out.print(' ');
		}
		System.out.println(end);
		Thread.sleep(delay / 2);
	}

	/**
	 * 파일에서 예외문자 제거하고 stringbuilder로 읽어오기
	 * 
	 * @param file 파일
	 * @param exceptionList 제외할 문자 리스트
	 * @param isRemoveDescription 주석 제거
	 * @return 개행문자가 포함되지 않은 stringbuilder
	 * @throws Exception
	 */
	public static StringBuilder getSourceCodeToStringBuilder(File file, List<String> exceptionList, boolean isRemoveDescription) throws Exception {
		// 파일 읽어오기
		StringBuilder sb = new StringBuilder();
		BufferedReader br = new BufferedReader(new FileReader(file));
		String s;
		while ((s = br.readLine()) != null) {
			sb.append(stringLineConvertToException(s, exceptionList, isRemoveDescription));
		}
		br.close();
		return sb;
	}

	public static void fileCopy(File source, File toCopy) {
		try {
			FileInputStream fis = new FileInputStream(source);
			FileOutputStream fos = new FileOutputStream(toCopy);
			int r;
			byte[] arr = new byte[1024];
			while ((r = fis.read(arr)) != -1) {
				fos.write(arr, 0, r);
			}
			fos.close();
			fis.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static int unZipIt(String zipFile, String outputFolder) throws Exception {
		int count = 0;
		byte[] buffer = new byte[1024];

		File folder = new File(outputFolder);
		if (!folder.exists()) {
			folder.mkdir();
		}

		ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile), Charset.forName("EUC-KR"));
		ZipEntry ze;

		List<String> files = new ArrayList<>();
		while ((ze = zis.getNextEntry()) != null) {
			files.add(ze.getName());
		}

		int minLength = files.get(0).length();
		String minStr = files.get(0);
		for (String s : files) {
			if (s.length() < minLength) {
				minLength = s.length();
				minStr = s;
			}
		}

		boolean isAllFileHasSameParent = true;
		for (String s : files) {
			if (!s.startsWith(minStr)) {
				isAllFileHasSameParent = false;
			}
		}

		zis.close();
		zis = new ZipInputStream(new FileInputStream(zipFile), Charset.forName("EUC-KR"));

		while ((ze = zis.getNextEntry()) != null) {
			String fileName = ze.getName();
			if (isAllFileHasSameParent && fileName.equals(minStr)) {
				// System.out.println("최상위에 폴더있음 : " + fileName);
				continue;
			} // 최상위에 있는 폴더 제거

			if (isAllFileHasSameParent) {
				fileName = fileName.substring(minStr.length() - 1);
			}

			File newFile = new File(outputFolder + File.separator + fileName);

			if (newFile.getPath().indexOf('.') == -1) {
				newFile.mkdirs();
				continue;
			} // 폴더일 경우 폴더 생성만 하고 넘어감

			if (!newFile.getParentFile().exists()) {
				newFile.getParentFile().mkdirs();
			}

			FileOutputStream fos = new FileOutputStream(newFile);

			int len;
			while ((len = zis.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}

			fos.close();
			count++;
		}

		zis.closeEntry();
		zis.close();

		// System.out.println("Done");
		return count;
	}
}
