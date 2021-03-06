package kr.dpmc.offlinejudgment.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.poi.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import kr.dpmc.offlinejudgment.MetaDataHW;
import kr.dpmc.offlinejudgment.MetaDataHW.File2;
import kr.dpmc.offlinejudgment.OJApi;
import kr.dpmc.offlinejudgment.YamlConfiguration;

public class HomeworkNormalization {

	public static void Normalization(YamlConfiguration config, int classNumber) throws Exception {
		File downloads = new File(config.getString("정규화.다운로드폴더"));
		// File normalizationDownloads = new File(config.getString("정규화.다운로드정리폴더"));
		File normalizationScore = new File(config.getString("채점.제출폴더"));
		List<MetaDataHW> metaDatas = new LinkedList<>();
		List<File> metaFiles = new LinkedList<>();

		loadHomeworkMetaData(metaDatas, metaFiles, downloads, normalizationScore, config);
		OJApi.printSwagWithStars("메타 파일 불러오기 성공", 50);
		// 메타 파일만 metaDatas로 불러옮

		// homeworkFileNormalization(metaDatas, normalizationDownloads);
		homeworkFileNormalizationWithUnzip(metaDatas, normalizationScore);
		OJApi.printSwagWithStars("과제 파일 정규화 완료", 50);
		// 다운로드정리폴더로 과제파일 복사하고 이름 정규화 함

		addNotAssignmentStudents(metaDatas, classNumber, config);
		OJApi.printSwagWithStars("미제출자 목록 확보", 50);
		// 미제출자 목록 metaDatas에 넣음

		OJApi.printSwagWithStars("엑셀 파일로 내보내기 시작", 50);
		SummarizeMetaDatas(metaDatas, config);
		OJApi.printSwagWithStars("제출 데이터 요약 결과 파일 생성", 50);
		// 제출 메타 데이터 엑셀로 요약해서 저장
	}

	public static void loadHomeworkMetaData(List<MetaDataHW> metaDatas, List<File> metaFiles, File downloads, File normalizationDownloads, YamlConfiguration config) {
		if (!downloads.exists())
			downloads.mkdirs();
		if (!normalizationDownloads.exists())
			normalizationDownloads.mkdirs();
		// 폴더 없으면 생성

		for (File file : downloads.listFiles()) {
			if (file.getName().matches("^[\\s\\w가-힣]+_\\d+_확인_\\d{4}-\\d{2}-\\d{2}-\\d{2}-\\d{2}-\\d{2}.txt")) {
				metaFiles.add(file);
				metaDatas.add(new MetaDataHW(file));
			}
		}
	}

	public static void homeworkFileNormalization(List<MetaDataHW> metaDatas, File normalizationDownloads) throws Exception {
		for (MetaDataHW meta : metaDatas) {
			if (meta.files != null) {
				if (meta.files.size() == 1) {
					File2 f2 = meta.files.get(0);
					String extension = OJApi.getFileExtension(f2.original.getName());
					File to = new File(normalizationDownloads, meta.id + " " + meta.name + "." + extension);
					File source = f2.newer;
					OJApi.fileCopy(source, to);
				} else if (meta.files.size() >= 2) {
					File toParent = new File(normalizationDownloads, meta.id + " " + meta.name);
					if (!toParent.exists()) {
						toParent.mkdirs();
					}
					for (File2 f2 : meta.files) {
						File to = new File(toParent, f2.original.getName());
						File source = f2.newer;
						OJApi.fileCopy(source, to);
					}
				} else {
					OJApi.printSwagWithAccent(meta.id + " " + meta.name + " 과제 제출 파일 0개임");
				}
			} // files null이면 메타 과제 데이터 처리 불가
		}
	}// 압축은 안풀음

	public static int moveFileWithUnzip(File older, File folder) throws Exception {
		// older이 zip파일이라면 newer파일이 아닌, newer폴더에 압축풀기
		int count = 0;// 옮긴 파일 갯수

		count += OJApi.unZipIt(older.getPath(), folder.getPath());
		for (File file : folder.listFiles()) {
			if (file.getName().endsWith(".zip")) {
				String newFolderName = file.getName().substring(0, file.getName().lastIndexOf('.'));
				File newFolder = new File(newFolderName);
				newFolder.mkdirs();
				count += moveFileWithUnzip(file, newFolder);
			}
		}

		return count;
	}

	public static void homeworkFileNormalizationWithUnzip(List<MetaDataHW> metaDatas, File normalizationDownloads) throws Exception {
		for (MetaDataHW meta : metaDatas) {
			if (meta.files != null) {
				if (meta.files.size() >= 1) {
					try {
						File toParent = new File(normalizationDownloads, meta.id + " " + meta.name);
						if (!toParent.exists()) {
							toParent.mkdirs();
						} // 학번 이름 폴더 생성

						if (meta.files.size() == 1) {
							meta.isSubmitFileZiped = meta.files.get(0).newer.getName().endsWith(".zip");
						} // 압축파일로 제출했는지 검사

						for (File2 f2 : meta.files) {
							if (f2.newer.getName().endsWith(".zip")) {
								int count = moveFileWithUnzip(f2.newer, toParent);
								if (count == 0) {
									count = 1;
								}
								meta.outputFilesCount += count;
								// zip파일이면 압축풀기
							} else {
								OJApi.fileCopy(f2.newer, new File(toParent, f2.original.getName()));
								meta.outputFilesCount += 1;
								// 아니면 그냥 파일 카피
							}

							// 블랙보드 파일에서 채점폴더로 이동
						}
					} catch (Exception e) {
						OJApi.printSwagWithAccent(meta.id + " " + meta.name + " 과제 제출 압축 해제 도중 오류 발생: \"" + meta.id + " " + meta.name + "\"폴더에 그대로 옮겼습니다.");
						File folder = new File(normalizationDownloads.getPath(), meta.id + " " + meta.name);
						folder.mkdirs();
						for (File2 file : meta.files) {
							OJApi.fileCopy(file.newer, new File(folder, file.original.getName()));
						}
					}

				} else {
					OJApi.printSwagWithAccent(meta.id + " " + meta.name + " 과제 제출 파일 0개임");
				}
			} // files null이면 메타 과제 데이터 처리 불가
		}
	}// 압축도 풀음

	public static void addNotAssignmentStudents(List<MetaDataHW> metaDatas, int classNumber, YamlConfiguration config) {
		if (classNumber > 0) {
			// classnum이 0,-1이면 이 과정 넘어감

			List<String[]> studentList = OJApi.getStudentList(config, classNumber);

			for (int i = 0; i < studentList.size(); i++) {
				String[] args = studentList.get(i);
				String id = args[0];
				String name = args[1];
				// System.out.println(id + " " + name + " 출석부에서 읽음");

				boolean isContains = false;
				for (MetaDataHW meta : metaDatas) {
					if (meta.id.equals(id))
						isContains = true;
				} // 기존 메타 데이터에 겹치는지 검사

				if (!isContains) {
					metaDatas.add(new MetaDataHW(id, name));
				} // 기존 메타 데이터에 없으므로 무시함
			}
			metaDatas.sort(OJApi.comparatorMeta);// 학번순으로 올림차순 정렬하는거
		}
	}

	public static void SummarizeMetaDatas(List<MetaDataHW> metaDatas, YamlConfiguration config) throws Exception {
		File excelFile = new File(config.getString("정규화.결과파일"));
		if (excelFile.exists()) {
			if (!excelFile.delete()) {
				OJApi.printSwagWithAccent("결과파일 삭제가 불가능합니다.");
			}
		}

		XSSFWorkbook workBook = new XSSFWorkbook();
		POIXMLProperties xmlProps = workBook.getProperties();
		POIXMLProperties.CoreProperties coreProps = xmlProps.getCoreProperties();
		coreProps.setCreator("DPmc");
		// 저자 설정

		XSSFSheet sheet = workBook.createSheet("meta");
		int rowIndex = 0;
		XSSFRow row = sheet.createRow(rowIndex);
		XSSFCell cell;
		String[] args = new String[] { "이름", "과제", "파일 갯수", "zip제출여부", "제출 날짜", "댓글" };
		// 제출날짜랑 제출필드는 없애자
		for (int i = 0; i < args.length; i++) {
			cell = row.createCell(i);
			cell.setCellValue(args[i]);
		} // 맨 윗줄 데이터 입력

		int num = 0;
		for (int i = 0; i < metaDatas.size(); i++) {
			MetaDataHW meta = metaDatas.get(i);
			row = sheet.createRow(i + 1);
			row.createCell(0).setCellValue(Integer.valueOf(meta.id));
			row.createCell(1).setCellValue(meta.name);
			row.createCell(2).setCellValue(meta.outputFilesCount);
			row.createCell(3).setCellValue(meta.isSubmitFileZiped);
			row.createCell(4).setCellValue(meta.submitDate);
			for (int j = 0; j < meta.comment.size(); j++) {
				row.createCell(5 + j).setCellValue(meta.comment.get(j));
			}
			num++;
			if (num % 10 == 0) {
				OJApi.printSwagWithStars(num + "명 작성 완료", 50);
			}
		} // 학생별 데이터 입력

		try {
			workBook.write(new FileOutputStream(excelFile));
			workBook.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
