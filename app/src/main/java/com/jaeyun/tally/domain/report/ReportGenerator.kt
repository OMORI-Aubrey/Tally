package com.jaeyun.tally.domain.report

import com.jaeyun.tally.domain.model.ReportSentence
import com.jaeyun.tally.domain.model.WeeklyReportInput

/**
 * 요약 문단 생성기 (§7.5). MVP 구현은 [TemplateReportGenerator] 하나다.
 *
 * LLM 구현체(§17.2)를 붙일 때 이 인터페이스만 구현한다. 그때 판정·목표처럼 더 필요한 사실은 [WeeklyReportInput]에,
 * 자유 문장은 [ReportSentence]에 종류를 더한다. 네트워크 호출을 위해 `suspend`로 둔다.
 */
interface ReportGenerator {
    suspend fun generate(input: WeeklyReportInput): List<ReportSentence>
}
