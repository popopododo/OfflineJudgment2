package kr.dpmc.offlinejudgment.main;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.hssf.record.CFRuleBase.ComparisonOperator;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFConditionalFormattingRule;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFPatternFormatting;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSheetConditionalFormatting;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import kr.dpmc.offlinejudgment.OJApi;
import kr.dpmc.offlinejudgment.StudentHW;
import kr.dpmc.offlinejudgment.TestData;
import kr.dpmc.offlinejudgment.YamlConfiguration;

public class ScoreCheck {

	private static Random random = new Random();

	/**
	 * 메인 채점 메서드
	 * 
	 * @param config 파일
	 * @param classNumber 분반 넘버
	 * @throws Exception
	 */
	public static void OfflineJudgmentMain(YamlConfiguration config, int classNumber) throws Exception {
		List<TestData> testDatas = getTestDatas(config);
		OJApi.printSwagWithStars("테스트 데이터 불러오기 완료 size=" + testDatas.size(), 50);
		// 테스트 데이터 설정

		Map<String, StudentHW> studentMap = checkStudentHwScore(testDatas, config);
		OJApi.printSwagWithStars("채점 완료", 50);
		// 학번, 학생정보 데이터

		writeToSimilirarityFiles(studentMap, config);
		OJApi.printSwagWithStars("유사도 검사용 파일 생성 완료", 50);
		// 유사도 검사용 파일로 코드만 내보내기
		
		addNotAssignmentStudent(studentMap, testDatas, classNumber, config);
		OJApi.printSwagWithStars("미제출자 데이터 확보", 50);
		// 미제출자 확보

		OJApi.printSwagWithStars("엑셀로 내보내기 시작", 50);
		writeToExcel(studentMap, testDatas, config);
		// writeToCSV(studentMap, testDatas, config);
		OJApi.printSwagWithStars("채점 결과 엑셀 파일로 저장 완료", 50);
		// 채점결과 저장
		
		copyToHWSumFolder(studentMap, config);
		OJApi.printSwagWithStars("코드파일모음폴더와 사진파일모음폴더로 내보내기 완료", 50);
	}

	/**
	 * 코드, 사진 모음 파일로 복사
	 * 
	 * @param studentMap
	 * @param config 채점.코드파일모음폴더, 채점.사진파일모음폴더
	 * @throws Exception
	 */
	public static void copyToHWSumFolder(Map<String, StudentHW> studentMap, YamlConfiguration config) throws Exception {
		String folder = config.getString("채점.코드파일모음폴더");
		File fFolder = new File(folder);
		if (!fFolder.exists()) {
			fFolder.mkdirs();
		}
		for (StudentHW student : studentMap.values()) {
			if (student.hwfile == null || student.fileName.equals("null")) {
				continue;
			}
			File toCopy = new File(fFolder.getPath() + "/" + student.id + " " + student.name + ".py");
			OJApi.fileCopy(student.hwfile, toCopy);
		} // 코드파일 모음

		folder = config.getString("채점.사진파일모음폴더");
		fFolder = new File(folder);
		if (!fFolder.exists()) {
			fFolder.mkdirs();
		}
		for (StudentHW student : studentMap.values()) {
			if (student.screenshotFiles == null || student.screenshotFiles.size() == 0) {
				continue;
			}
			int index = 1;
			for (File file : student.screenshotFiles) {
				String extension = OJApi.getFileExtension(file.getName());
				File toCopy = new File(fFolder.getPath() + "/" + student.id + " " + student.name + " " + index + "." + extension);
				OJApi.fileCopy(file, toCopy);
			}
		} // 사진파일 모음

	}

	public static void addNotAssignmentStudent(Map<String, StudentHW> studentMap, List<TestData> testDatas, int classNumber, YamlConfiguration config) {
		if (classNumber > 0) {
			List<String[]> studentList = OJApi.getStudentList(config, classNumber);
			List<String> idList = new ArrayList<>(studentMap.keySet());

			for (int i = 0; i < studentList.size(); i++) {
				String[] args = studentList.get(i);
				String id = args[0];
				String name = args[1];

				if (!idList.contains(id)) {
					StudentHW student = new StudentHW(name, id);
					student.homeworkFileScore = 1;
					student.screenshotScore = 1;
					student.fileName = "null";
					student.testcaseScore = new int[testDatas.size()];
					for (int j = 0; j < student.testcaseScore.length; j++) {
						student.testcaseScore[j] = 9;
					}
					studentMap.put(id, student);
					idList.add(id);
				} // 학생 목록에 없다면
			}

			Collections.sort(idList, OJApi.comparatorString);

			Map<String, StudentHW> map = new LinkedHashMap<>();
			for (String id : idList) {
				map.put(id, studentMap.get(id));
			}

			studentMap.clear();
			for (String id : map.keySet()) {
				studentMap.put(id, map.get(id));
			}
			// studentMap 정렬 완료
		}
	}

	public static void writeToSimilirarityFiles(Map<String, StudentHW> studentMap, YamlConfiguration config) throws Exception {
		File similirarity = new File(config.getString("유사도.제출폴더"));
		if (!similirarity.exists()) {
			similirarity.mkdirs();
		} // 유사도 검사 폴더

		for (StudentHW student : studentMap.values()) {
			if (student.fileName == null || student.fileName.equals("null")) {
				OJApi.printSwagWithAccent(student.id + " " + student.name + "학생은 유사도 검사파일 생성 불가");
				continue;
			}

			if (student.fileName.equals("")) {
				continue;
			} // 미제출자 넘김

			File file = new File(similirarity.getPath() + "/" + student.id + ".py");
			if (!file.exists()) {
				file.createNewFile();
			}

			BufferedReader br = new BufferedReader(new FileReader(student.hwfile));
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			String s;
			while ((s = br.readLine()) != null) {
				s = OJApi.stringLineConvertToException(s, config.getStringList("유사도.제외문자"), config.getBoolean("유사도.주석제거"));

				if (config.getBoolean("유사도.탭제거")) {
					s = s.replace("\t", "");
				} // 탭 제거하고 trim

				s = s.trim();

				bw.write(s);

				if (!config.getBoolean("유사도.줄넘김제거")) {
					bw.write('\n');
				} // 줄넘김 제거

			}
			bw.close();
			br.close();

		}
	}

	public static Map<String, StudentHW> checkStudentHwScore(List<TestData> testDatas, YamlConfiguration config) throws Exception {
		Map<String, StudentHW> studentMap = new LinkedHashMap<>();
		List<String> checkExceptionChar = config.getStringList("채점.채점제외문자");
		List<String> hwRecognizeList = config.getStringList("채점.과제인식문자");
		List<String> screenshotExtension = config.getStringList("채점.사진인식확장자");
		List<String> sourcecodeCheck = config.getStringList("채점.소스파일검사");

		File parentFolder = new File(config.getString("채점.제출폴더"));
		if (!parentFolder.exists()) {
			parentFolder.mkdir();
		} // 폴더 없으면 만들기
		File instFolder = new File("출력");
		instFolder.mkdirs();
		int num = 0;
		for (File folder : parentFolder.listFiles()) {
			// 학생별 제출 폴더 = folder

			if (!folder.isDirectory()) {
				OJApi.printSwagWithBraket(folder.getName() + "은 폴더가 아님");
				continue;
			}
			// 폴더가 아닐 경우

			String fname = folder.getName();
			if (!fname.matches("[0-9]+ [0-9가-힣]+")) {
				OJApi.printSwagWithBraket(fname + " 폴더에서 학생 이름과 학번 인식 불가능");
				continue;
			}
			// 정규식 일치하지 않을 경우

			String[] args = fname.split(" ");
			if (args.length != 2) {
				OJApi.printSwagWithBraket(Arrays.toString(args) + " 폴더 이름에서 학생과 학번외에 다른 정보가 있음");
				continue;
			}
			// 정보가 너무 많을 경우

			String id = args[0];
			String name = args[1];
			StudentHW student = new StudentHW(name, id);
			studentMap.put(id, student);
			// 학생 이름과 학번 정보 분리

			// 사진 파일 추출
			student.screenshotFiles = new LinkedList<>();
			for (File file : folder.listFiles()) {
				boolean isEndwith = false;
				String fileName = file.getName().toLowerCase();
				for (String extens : screenshotExtension) {
					if (fileName.endsWith(extens)) {
						isEndwith = true;
						break;
					}
				}
				if (isEndwith) {
					student.screenshotFiles.add(file);
				}
			}

			if (student.screenshotFiles.size() >= 1) {
				student.screenshotScore = 2;
				// 사진 파일 있음
			} else {
				student.screenshotScore = 1;
				// 사진 파일 없음
			}

			// 과제 파일을 찾았는가
			boolean isFindHW = false;
			for (File hwFile : folder.listFiles()) {
				isFindHW = false;
				if (hwFile.getName().endsWith(".py")) {
					isFindHW = true;
					StringBuilder sb = OJApi.getSourceCodeToStringBuilder(hwFile, config.getStringList("채점.과제인식제외문자"), true);
					for (String s : hwRecognizeList) {
						if (sb.indexOf(s) == -1) {
							isFindHW = false;
							break;
						}
					} // 과제 파일 찾기 for

					if (isFindHW) {
						student.fileName = hwFile.getName();
						student.hwfile = hwFile;
						break;
					} // 원하는 파일을 찾았을 경우
				}
			}
			// 과제 파일 찾기

			num++;
			if (num % 10 == 0) {
				//OJApi.printSwagWithBraket(num + "명 채점 완료");
			} // 출력용 메세지

			if (isFindHW) {
				student.testcaseScore = new int[testDatas.size()];
				for (int i = 0; i < testDatas.size(); i++) {
					student.testcaseScore[i] = 9;
				}
				student.homeworkFileScore = 3;
				//System.out.print(student.id + " " + student.name + "->");
				//System.out.println(student.hwfile);
				StringBuilder sb = OJApi.getSourceCodeToStringBuilder(student.hwfile, null, true);
				boolean hasSourceCodeString = true; // 소스코드에 특정 문자열 있는지 알려주는 변수
				for (String str : sourcecodeCheck) {
					if (sb.indexOf(str) == -1) {
						hasSourceCodeString = false;
					}
				}
				if (hasSourceCodeString) {
					student.sourcecodeScore = 2;
				} else {
					student.sourcecodeScore = 1;
				}
				// 과제 파일 찾으면
			} else {
				OJApi.printSwagWithAccent(id + " " + name + "폴더에서 과제 파일 찾지 못함");
				student.testcaseScore = new int[testDatas.size()];
				for (int i = 0; i < testDatas.size(); i++) {
					student.testcaseScore[i] = 9;
				}
				student.homeworkFileScore = 2;
				student.sourcecodeScore = 0;
				student.hwfile = null;
				student.fileName = "null";
				continue;
				// 과제파일을 찾지 못해서 넘어감
			}

			double score = 0;
			File originalFile = null;
			if (config.getBoolean("채점.입력프롬프트메세지제거")) {
				originalFile = student.hwfile;

				String pathf = student.hwfile.getPath();
				String namef = student.hwfile.getName();
				pathf = pathf.replace(namef, "");
				pathf = pathf + File.separator + random.nextInt(1000000) + ".py";
				student.hwfile = new File(pathf);

				BufferedReader br = new BufferedReader(new FileReader(originalFile));
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(student.hwfile), "Cp949"));
				String s;
				while ((s = br.readLine()) != null) {
					s = OJApi.stringLineConvertToException(s, null, false);
					if (s.contains("input")) {
						int i1 = s.indexOf("input") + "input".length();
						boolean isOpenQuotes = false;
						int countOpenBracket = 0;
						int endIndex = -1;
						for (int i = i1; i < s.length(); i++) {
							char c = s.charAt(i);
							if (isOpenQuotes) {
								if (s.charAt(i - 1) != '\\' && (c == '\"' || c == '\'')) {
									// \"일 경우 무시하고 " 일때만 작동
									isOpenQuotes = false;
								}
							} else {
								if (c == '(') {
									countOpenBracket++;
									// 열림 기호시 1증가
								} else if (c == '\"' || c == '\'') {
									isOpenQuotes = true;
								} else if (c == ')') {
									// 닫힘 기호시 1감소
									countOpenBracket--;
									if (countOpenBracket == 0) {
										endIndex = i;
										break;
									}
								}
							}
						}

						if (endIndex == -1) {
							//System.out.println("소스 불량 " + student.id + " " + student.name);
							//System.out.println("s=[" + s + "]");
							bw.append(s).append('\n');
						} else {
							String s2 = s.substring(0, i1 + 1);
							String s3 = s.substring(endIndex);
							//System.out.println("소스 정상 " + student.id + " " + student.name);
							//System.out.println("s=[" + s2 + s3 + "]");
							bw.append(s2).append(s3).append('\n');
						}
					} else {
						bw.append(s).append('\n');
					}
				}
				br.close();
				bw.close();
			}
			
			FileWriter fw = new FileWriter(instFolder + "/" + student.id + " " + student.name + ".txt");
			for (int i = 0; i < testDatas.size(); i++) {
				TestData testData = testDatas.get(i);
				//알림점
				List<String> list = new ArrayList<>();
				int code = ScoreCheck.check(student.hwfile.getPath(), testData, checkExceptionChar, list);
				fw.append("TestCase: " + (i+1)).append('\n');
				for(String s : list){
					fw.append(s).append('\n');
				}
				fw.append('\n');
				student.testcaseScore[i] = code;
				if (code == 7)
					score++;
			} // 테스트 케이스별로 채점하는것
			fw.close();
			score /= testDatas.size();
			student.totalScore = score;

			if (config.getBoolean("채점.입력프롬프트메세지제거")) {
				student.hwfile.delete();
				student.hwfile = originalFile;
			}

			// 학생 한명(파일 하나) 끝
		}
		return studentMap;
	}

	@SuppressWarnings({ "unused", "deprecation" })
	public static List<TestData> getTestDatas(YamlConfiguration config) throws Exception {
		List<TestData> testDatas = new ArrayList<>();
		File testDataFolder = new File(config.getString("채점.입출력폴더"));
		if (!testDataFolder.exists()) {
			testDataFolder.mkdirs();
		} // 테스트 데이터 가져오기

		if (testDataFolder.listFiles().length == 0) {
			OJApi.printSwagWithAccent("테스트 데이터가 없습니다. 상세 설명을 읽어주세요.");
			return null;
		} // 테스트 데이터 없으므로 종료

		if (false) {
			Set<String> testDataNames = new LinkedHashSet<>();
			for (File file : testDataFolder.listFiles()) {
				String name = file.getName();
				if (name.endsWith(".in") || name.endsWith(".outlow") || name.endsWith(".outhigh")) {
					int i1 = name.lastIndexOf('.');
					testDataNames.add(name.substring(0, i1));
				} else {
					
					System.out.println(name + "은 정해진 파일 확장자가 아닙니다. (.in .outlow .outhigh)");
				}
			} // 테스트 데이터 목록들만 가져오기

			String parentPath = testDataFolder.getPath();
			for (String name : testDataNames) {
				File in = new File(parentPath + "/" + name + ".in");
				File outraw = new File(parentPath + "/" + name + ".outlow");
				File outhigh = new File(parentPath + "/" + name + ".outhigh");
				if (in.exists() && outraw.exists() && outhigh.exists()) {
					testDatas.add(new TestData(in, outraw, outhigh));
				} else {
					System.out.println(name + " 테스트 데이터는 .in .outlow .outhigh 확장자가 전부 존재하지 않습니다.");
				}
			}
		} // 레거시 코드 (빌드6 이하)

		for (File file : testDataFolder.listFiles()) {
			if (file.getName().endsWith(".testcase")) {
				testDatas.add(new TestData(file));
			} else {
				OJApi.printSwagWithAccent(file.getName() +"은 정해진 파일 확장자가 아닙니다. (.case)");
			}

		} // test data로드 빌드7이상

		return testDatas;
	}

	public static void writeToExcel(Map<String, StudentHW> map, List<TestData> testDatas, YamlConfiguration config) throws Exception {
		XSSFWorkbook workBook = new XSSFWorkbook();
		POIXMLProperties xmlProps = workBook.getProperties();
		POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
		coreProps.setCreator("DPmc");
		// 저자 설정

		XSSFSheet sheet = workBook.createSheet("결과");
		
		XSSFRow row = sheet.createRow(0);
		String[] args = new String[] { "학번", "이름", "파일이름", "통합점수", "파일점수", "사진점수", "소스코드점수" };
		String[] args2 = new String[] { "결과 요약", "점수", "코멘트", "비고" };

		int rowIndex = 1;
		int columnIndex = 0;
		columnIndex = setCellValueForObject(row, columnIndex, args);
		for (int i = 0; i < testDatas.size(); i++) {
			row.createCell(columnIndex).setCellValue("케이스" + (i + 1));
			columnIndex++;
		}
		columnIndex = setCellValueForObject(row, columnIndex, args2);

		XSSFCellStyle style = workBook.createCellStyle();
		XSSFDataFormat format = workBook.createDataFormat();
		style.setDataFormat(format.getFormat("0.0\"%\""));
		// 퍼센트기호 붙이는 서식
		// 통합점수 서식인데 없앰

		int num = 0;
		for (StudentHW std : map.values()) {
			row = sheet.createRow(rowIndex);
			rowIndex++;

			columnIndex = 0;
			columnIndex = setCellValueForObject(row, columnIndex, std.id);
			columnIndex = setCellValueForObject(row, columnIndex, std.name);
			columnIndex = setCellValueForObject(row, columnIndex, std.fileName);
			columnIndex = setCellValueForObject(row, columnIndex, getTotalScore(std));// 통합점수
			columnIndex = setCellValueForObject(row, columnIndex, std.homeworkFileScore);
			columnIndex = setCellValueForObject(row, columnIndex, std.screenshotScore);
			columnIndex = setCellValueForObject(row, columnIndex, std.sourcecodeScore);
			// row.getCell(columnIndex - 1).setCellStyle(style);
			// args1 데이터들 입력

			columnIndex = setCellValueForObject(row, columnIndex, std.testcaseScore);
			// 테스트 케이스 입력

			boolean isPerfectCode = true;
			boolean isNotAssignment = false;
			boolean isCantFindPYFile = false;
			isNotAssignment = std.homeworkFileScore == 1;
			isCantFindPYFile = std.homeworkFileScore == 2;
			for (int i = 0; i < std.testcaseScore.length; i++) {
				int code = std.testcaseScore[i];
				if (code != 7)
					isPerfectCode = false;
			} // 결과 판단 요약

			String result;
			if (isNotAssignment) {
				result = "과제 미제출";
			} else if (isCantFindPYFile) {
				result = "코드파일 못찾음";
			} else if (isPerfectCode) {
				result = "정상 채점";
			} else {
				result = "재검사 필요";
			}
			columnIndex = setCellValueForObject(row, columnIndex, result);
			// 결과 요약 입력

			num++;
			if (num % 10 == 0) {
				// System.out.println(" *** " + num + "명 엑셀로 작성 완료 ***");
			}
		}
		OJApi.printSwagWithStars("채점 데이터 작성 완료", 50);

		{
			StringBuilder totalDes2 = new StringBuilder("322");
			for (int i = 0; i < testDatas.size(); i++) {
				totalDes2.append("7");
			}
			String[] totalscoreDescription = { "뒤의 점수 코드를 합친 셀", totalDes2.toString() + "이 완벽한 정답코드" };// index 3
			String[] fileDescription = { "0: null", "1: 과제 미제출", "2: 코드파일 못찾음", "3: 코드파일 찾음" };// index 4
			String[] screenshotDescription = { "0: null", "1: 사진파일 못찾음", "2: 사진파일 찾음" };// index 5
			String[] codeDescription = { "0: null", "1: 코드에 특정 문자열 없음", "2: 코드에 특정 문자열 있음" };// index 6
			String[] testcaseDescription = { "low/high/equal을 순서대로 한 자리씩 할당함", "0: 000 (전원 불일치)", "1: 001", "2: 010", "3: 011", "4: 100", "5: 101", "6: 110", "7: 111 (전원 일치)", "8: 파이썬 오류 발생", "9: null" }; // 7
			String[] colorDescription = {"주황: 7(111)이 아닌 것들", "빨강: 9(파일없음)", "초록: 100점이 아닌 것들"};
			
			rowIndex += 5;
			//5칸 내려감
			
			for (int i = rowIndex; i < rowIndex + testcaseDescription.length; i++) {
				sheet.createRow(i);
			}
			// testcase설명이 가장 기니까 그 길이를 사용함

			sheet.setColumnWidth(2, (int)(sheet.getColumnWidth(2) * 2.5));
			sheet.setColumnWidth(3, sheet.getColumnWidth(3) * 2);
			sheet.setColumnWidth(2, (int)(sheet.getColumnWidth(4) * 1.5));
			sheet.setColumnWidth(2, (int)(sheet.getColumnWidth(5) * 1.5));
			sheet.setColumnWidth(2, (int)(sheet.getColumnWidth(6) * 1.5));
			
			int resultIndex = args.length + testDatas.size();
			sheet.setColumnWidth(resultIndex, (int)(sheet.getColumnWidth(resultIndex) * 1.75));
			//길이 설정
			
			
			setColumnValueForStringArray(sheet, rowIndex, 3, totalscoreDescription);
			setColumnValueForStringArray(sheet, rowIndex, 4, fileDescription);
			setColumnValueForStringArray(sheet, rowIndex, 5, screenshotDescription);
			setColumnValueForStringArray(sheet, rowIndex, 6, codeDescription);
			setColumnValueForStringArray(sheet, rowIndex, 7, testcaseDescription);
			setColumnValueForStringArray(sheet, rowIndex, 8, colorDescription);
		}

		XSSFSheetConditionalFormatting sheetcf = sheet.getSheetConditionalFormatting();
		// 조건부서식 생성자
		
		{
			XSSFConditionalFormattingRule rule = sheetcf.createConditionalFormattingRule(ComparisonOperator.EQUAL, "9");
			XSSFPatternFormatting patternFormat = rule.createPatternFormatting();
			patternFormat.setFillBackgroundColor(IndexedColors.RED.index);
			// 조건부 서식 생성

			CellRangeAddress[] arr = { new CellRangeAddress(1, map.size(), args.length, args.length + testDatas.size() - 1) };
			sheetcf.addConditionalFormatting(arr, rule);
			// 조건부 서식 입력
		} // 테스트 케이스에 빨강 색상 입력
		
		{
			XSSFConditionalFormattingRule rule = sheetcf.createConditionalFormattingRule(ComparisonOperator.NOT_EQUAL, "7");
			XSSFPatternFormatting patternFormat = rule.createPatternFormatting();
			patternFormat.setFillBackgroundColor(IndexedColors.LIGHT_ORANGE.index);
			// 조건부 서식 생성

			CellRangeAddress[] arr = { new CellRangeAddress(1, map.size(), args.length, args.length + testDatas.size() - 1) };
			sheetcf.addConditionalFormatting(arr, rule);
			// System.out.println("주황=" + arr[0].formatAsString());
			// 조건부 서식 입력
		} // 테스트 케이스에 주황 색상 입력

		{
			XSSFConditionalFormattingRule rule = sheetcf.createConditionalFormattingRule(ComparisonOperator.NOT_EQUAL, "\"정상 채점\"");
			XSSFPatternFormatting patternFormat = rule.createPatternFormatting();
			patternFormat.setFillBackgroundColor(new XSSFColor(new Color(146, 208, 80)));
			// 조건부 서식 생성

			CellRangeAddress[] arr = { new CellRangeAddress(1, map.size(), args.length + testDatas.size(), args.length + testDatas.size()) };
			sheetcf.addConditionalFormatting(arr, rule);
			// System.out.println("초록=" + arr[0].formatAsString());
			// 조건부 서식 입력
		}
		OJApi.printSwagWithStars("조건부 서식 설정 완료", 50);

		File excelFile = new File(config.getString("채점.결과파일"));
		if (excelFile.exists())
			excelFile.delete();
		workBook.write(new FileOutputStream(excelFile));
		workBook.close();
	}

	public static long getTotalScore(StudentHW std) {
		long l = 0;
		l *= 10;
		l += std.homeworkFileScore;

		l *= 10;
		l += std.screenshotScore;

		l *= 10;
		l += std.sourcecodeScore;

		for (int i = 0; i < std.testcaseScore.length; i++) {
			l *= 10;
			l += std.testcaseScore[i];
		}
		return l;
	}

	public static void setColumnValueForStringArray(XSSFSheet sheet, int rowIndex, int columnIndex, String[] arr) {
		for (int i = 0; i < arr.length; i++) {
			XSSFRow row = sheet.getRow(rowIndex + i);
			XSSFCell cell = row.createCell(columnIndex);
			cell.setCellValue(arr[i]);
		}
	}

	public static int setCellValueForObject(XSSFRow row, int columnIndex, Object obj) {
		if (obj instanceof int[]) {
			int[] arr = (int[]) obj;
			for (int i = 0; i < arr.length; i++) {
				row.createCell(columnIndex).setCellValue(arr[i]);
				columnIndex++;
			}
		} else if (obj instanceof String[]) {
			String[] arr = (String[]) obj;
			for (int i = 0; i < arr.length; i++) {
				row.createCell(columnIndex).setCellValue(arr[i]);
				columnIndex++;
			}
		} else if (obj instanceof Double) {
			row.createCell(columnIndex).setCellValue((double) obj);
			columnIndex++;
		} else if (obj instanceof String) {
			row.createCell(columnIndex).setCellValue((String) obj);
			columnIndex++;
		} else if (obj instanceof Integer) {
			row.createCell(columnIndex).setCellValue((int) obj);
			columnIndex++;
		} else if (obj instanceof Long) {
			row.createCell(columnIndex).setCellValue((long) obj);
			columnIndex++;
		}
		return columnIndex;
	}

	public static boolean IS_DEBUG = false;

	/**
	 * 학생 1명 과체 체크
	 * 
	 * @param filePath 파일 위치
	 * @param testData 테스트 데이터 모음
	 * @param outputException 채점시 제외할 문자들
	 * @return 채점코드
	 * @throws IOException
	 */
	public static int check(String filePath, TestData testData, List<String> outputException, List<String> output) throws IOException {
		Process pc = Runtime.getRuntime().exec("python \"" + filePath + "\"");

		OutputStream os = pc.getOutputStream();
		InputStream is = pc.getInputStream();
		InputStream es = pc.getErrorStream();

		for (int i = 0; i < testData.input.size(); i++) {
			os.write((testData.input.get(i) + "\n").getBytes());
			if (IS_DEBUG)
				System.out.println("[Std in] " + testData.input.get(i));
		}
		os.flush();
		os.close();
		// input 데이터 입력

		List<String> outLines = new ArrayList<>();
		List<String> errLines = new ArrayList<>();
		{
			String line;
			BufferedReader brCleanUp = new BufferedReader(new InputStreamReader(is));
			while ((line = brCleanUp.readLine()) != null) {
				output.add(line);
				outLines.add(OJApi.stringLineConvertToException(line, outputException, false));// 키워드
																								// 제거
				if (IS_DEBUG)
					System.out.println("[Stdout] " + line);
			}
			brCleanUp.close();
			// output 입력받기

			brCleanUp = new BufferedReader(new InputStreamReader(es));
			while ((line = brCleanUp.readLine()) != null) {
				errLines.add(line);
				if (IS_DEBUG)
					System.out.println("[Stderr] " + line);
			}
			brCleanUp.close();
			// 에러 메세지 입력받기
		}

		boolean isLowChecked = false;
		boolean isHighChecked = false;
		boolean isEqualChecked = false;

		isLowChecked = listContainsString(outLines, testData.outputLow);
		isHighChecked = listContainsString(outLines, testData.outputHigh);
		isEqualChecked = listEqualsString(outLines, testData.outputEqual);

		int code = 0;
		if (isLowChecked)
			code += 100;
		if (isHighChecked)
			code += 10;
		if (isEqualChecked)
			code += 1;
		// code - low/high/equal 각각 1자리씩 여부

		if (errLines.size() >= 1) {
			// python 오류
			return 8;
		} else if (code == 000) {
			return 0;
		} else if (code == 001) {
			return 1;
		} else if (code == 010) {
			return 2;
		} else if (code == 011) {
			return 3;
		} else if (code == 100) {
			return 4;
		} else if (code == 101) {
			return 5;
		} else if (code == 110) {
			return 6;
		} else if (code == 111) {
			return 7;
		} else {
			return 9;
		}
	}

	/**
	 * outLines안에 testData가 순서대로 있는지 검사
	 * 
	 * @param outLines 검사될 문자 리스트
	 * @param testData 검사할 문자 리스트
	 * @return contains 여부
	 */
	public static boolean listContainsString(List<String> outLines, List<String> testData) {
		if (testData.size() == 0) {
			return true;
		}

		boolean isContains = false;
		int index = 0;
		for (String line : outLines) {
			String compareStr = testData.get(index);
			if (line.contains(compareStr)) {
				index++;
			}
			if (index == testData.size()) {
				isContains = true;
				break;
			}
		}
		return isContains;
	}

	/**
	 * outLines안에 testData가 순서대로 있는지 검사
	 * 
	 * @param outLines 검사될 문자 리스트
	 * @param testData 검사할 문자 리스트
	 * @return equals 여부
	 */
	public static boolean listEqualsString(List<String> outLines, List<String> testData) {
		if (testData.size() == 0) {
			return true;
		}

		boolean isEqual = false;
		int index = 0;
		for (String line : outLines) {
			String compareStr = testData.get(index);
			if (line.equals(compareStr)) {
				index++;
			}
			if (index == testData.size()) {
				isEqual = true;
				break;
			}
		}
		return isEqual;
	}
}
