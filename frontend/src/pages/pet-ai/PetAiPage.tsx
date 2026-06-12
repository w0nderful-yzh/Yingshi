import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Empty, Input, Select, Spin, message, Tooltip } from 'antd';
import {
  Bell,
  BrainCircuit,
  Camera,
  Clock3,
  FileText,
  Image as ImageIcon,
  RefreshCw,
  ScanSearch,
  ShieldCheck,
  Sparkles,
  TriangleAlert,
  CalendarDays,
  CalendarRange,
} from 'lucide-react';
import { useSearchParams } from 'react-router-dom';
import {
  generatePetAiReport,
  getPetAiReports,
  generateDailySummary,
  generateWeeklySummary,
} from '@/api/petAi';
import { getPets } from '@/api/pet';
import type {
  PetAiReportQuery,
  PetAiReportVO,
  PetAiRiskLevel,
  PetAiSourceType,
  PetVO,
} from '@/types';
import { formatDate } from '@/utils/format';
import { PetTypeMap } from '@/utils/constants';

const { TextArea } = Input;

const riskMeta: Record<PetAiRiskLevel, { label: string; note: string }> = {
  LOW: { label: '低风险', note: '未发现明显异常' },
  MEDIUM: { label: '需关注', note: '建议继续观察' },
  HIGH: { label: '高风险', note: '建议尽快确认现场' },
};

const sourceMeta: Record<PetAiSourceType, { label: string; icon: typeof Bell }> = {
  ALARM: { label: '告警事件', icon: Bell },
  DETECTION: { label: '检测记录', icon: ScanSearch },
  IMAGE: { label: '手动图像', icon: Camera },
  DAILY_SUMMARY: { label: '每日报告', icon: CalendarDays },
  WEEKLY_SUMMARY: { label: '每周报告', icon: CalendarRange },
};

function ReportDetail({ report }: { report: PetAiReportVO }) {
  const SourceIcon = sourceMeta[report.sourceType]?.icon ?? FileText;
  return (
    <article className="ai-report">
      <div className="ai-report__hero">
        {report.imageUrl && (
          <div className="ai-report__image-wrap">
            <img src={report.imageUrl} alt={`${report.petName}的监控证据`} />
            <div className="ai-report__image-label">
              <Camera size={13} />
              原始证据画面
            </div>
          </div>
        )}
        <div className="ai-report__summary">
          <div className={`ai-risk ai-risk--${report.riskLevel.toLowerCase()}`}>
            {report.riskLevel === 'HIGH' ? <TriangleAlert size={16} /> : <ShieldCheck size={16} />}
            <span>{riskMeta[report.riskLevel].label}</span>
          </div>
          <div className="ai-report__source">
            <SourceIcon size={14} />
            {sourceMeta[report.sourceType]?.label ?? report.sourceType}
            {report.sourceId ? ` #${report.sourceId}` : ''}
          </div>
          <h2>{report.title}</h2>
          <p>{report.summary}</p>
          <div className="ai-report__meta">
            <span><strong>{report.petName}</strong><small>分析对象</small></span>
            <span><strong>{report.modelName}</strong><small>分析模型</small></span>
            <span><strong>{formatDate(report.sourceTime || report.createdAt)}</strong><small>事件时间</small></span>
          </div>
        </div>
      </div>

      <div className="ai-report__sections">
        <section>
          <div className="ai-section-title"><ScanSearch size={16} /><span>画面观察</span></div>
          <p>{report.observedBehavior}</p>
        </section>
        <section>
          <div className="ai-section-title"><FileText size={16} /><span>判断依据</span></div>
          <p>{report.evidenceBasis}</p>
        </section>
        <section className="ai-report__recommendations">
          <div className="ai-section-title"><ShieldCheck size={16} /><span>行动建议</span></div>
          <ol>
            {report.recommendations.map((item, index) => (
              <li key={`${item}-${index}`}><span>{index + 1}</span>{item}</li>
            ))}
          </ol>
        </section>
        <section className="ai-report__uncertainties">
          <div className="ai-section-title"><TriangleAlert size={16} /><span>分析边界</span></div>
          <ul>
            {report.uncertainties.map((item, index) => <li key={`${item}-${index}`}>{item}</li>)}
          </ul>
        </section>
      </div>

      <details className="ai-report__evidence">
        <summary>查看系统证据包</summary>
        <pre>{JSON.stringify(report.evidence, null, 2)}</pre>
      </details>
      <footer>
        AI结果用于辅助判断，不构成医疗诊断。安全区状态和告警时间以系统检测记录为准。
      </footer>
    </article>
  );
}

export default function PetAiPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [reports, setReports] = useState<PetAiReportVO[]>([]);
  const [pets, setPets] = useState<PetVO[]>([]);
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [selectedId, setSelectedId] = useState<number>();
  const [filters, setFilters] = useState<PetAiReportQuery>({});
  const [petId, setPetId] = useState<number>();
  const [imageUrl, setImageUrl] = useState('');
  const [question, setQuestion] = useState('');

  const selectedReport = useMemo(
    () => reports.find((item) => item.id === selectedId) || reports[0],
    [reports, selectedId],
  );

  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getPetAiReports(filters);
      setReports(data);
      const queryId = Number(searchParams.get('reportId'));
      if (queryId && data.some((item) => item.id === queryId)) {
        setSelectedId(queryId);
      } else if (!selectedId || !data.some((item) => item.id === selectedId)) {
        setSelectedId(data[0]?.id);
      }
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setLoading(false);
    }
  }, [filters, searchParams, selectedId]);

  useEffect(() => {
    getPets().then(setPets).catch(() => {});
  }, []);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const handleSelect = (id: number) => {
    setSelectedId(id);
    setSearchParams({ reportId: String(id) });
  };

  const handleManualGenerate = async () => {
    if (!petId || !imageUrl.trim()) {
      message.warning('请选择宠物并填写可访问的图片地址');
      return;
    }
    setGenerating(true);
    try {
      const report = await generatePetAiReport({
        sourceType: 'IMAGE',
        petId,
        imageUrl: imageUrl.trim(),
        question: question.trim() || undefined,
      });
      setReports((current) => [report, ...current]);
      handleSelect(report.id);
      setImageUrl('');
      setQuestion('');
      message.success('多模态分析报告已生成');
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setGenerating(false);
    }
  };

  const handleSummary = async (type: 'daily' | 'weekly') => {
    if (!petId) {
      message.warning('请先选择宠物');
      return;
    }
    setGenerating(true);
    try {
      const fn = type === 'daily' ? generateDailySummary : generateWeeklySummary;
      const report = await fn(petId);
      setReports((current) => [report, ...current]);
      handleSelect(report.id);
      message.success(type === 'daily' ? '日报已生成' : '周报已生成');
    } catch (err: any) {
      message.error(err.message);
    } finally {
      setGenerating(false);
    }
  };

  const highRiskCount = reports.filter((item) => item.riskLevel === 'HIGH').length;
  const todayCount = reports.filter((item) => {
    const date = new Date(item.createdAt);
    const today = new Date();
    return date.toDateString() === today.toDateString();
  }).length;
  const summaryCount = reports.filter((item) =>
    item.sourceType === 'DAILY_SUMMARY' || item.sourceType === 'WEEKLY_SUMMARY'
  ).length;

  return (
    <div className="ai-center">
      <header className="ai-center__header">
        <div>
          <div className="ai-center__eyebrow"><Sparkles size={12} /> MULTIMODAL EVIDENCE DESK</div>
          <h1>宠物 AI 分析中心</h1>
          <p>把萤石告警、检测事实和宠物档案组织成可追溯的事件报告。</p>
        </div>
        <Button icon={<RefreshCw size={14} />} onClick={loadReports} loading={loading}>刷新记录</Button>
      </header>

      <div className="ai-center__stats">
        <div><BrainCircuit size={18} /><span><small>累计报告</small><strong>{reports.length}</strong></span></div>
        <div><TriangleAlert size={18} /><span><small>高风险事件</small><strong>{highRiskCount}</strong></span></div>
        <div><Clock3 size={18} /><span><small>今日生成</small><strong>{todayCount}</strong></span></div>
        <div><CalendarDays size={18} /><span><small>活动总结</small><strong>{summaryCount}</strong></span></div>
      </div>

      <section className="ai-compose">
        <div className="ai-compose__intro">
          <span><ImageIcon size={17} /></span>
          <div><strong>分析一张新画面</strong><small>告警和检测记录可直接从对应页面发起</small></div>
        </div>
        <Select
          value={petId}
          onChange={setPetId}
          placeholder="选择宠物"
          options={pets.map((pet) => ({
            value: pet.id,
            label: `${pet.petName} · ${PetTypeMap[pet.petType] || pet.petType}`,
          }))}
        />
        <Input value={imageUrl} onChange={(e) => setImageUrl(e.target.value)} placeholder="萤石截图或公网图片 URL" />
        <TextArea value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="希望 AI 重点关注什么？（可选）" autoSize={{ minRows: 1, maxRows: 2 }} />
        <div className="ai-compose__actions">
          <Button type="primary" icon={<Sparkles size={15} />} loading={generating} onClick={handleManualGenerate}>生成报告</Button>
          <Tooltip title="汇总今日检测数据，生成活动日报">
            <Button icon={<CalendarDays size={15} />} loading={generating} onClick={() => handleSummary('daily')}>生成日报</Button>
          </Tooltip>
          <Tooltip title="汇总近7天检测数据，生成活动周报">
            <Button icon={<CalendarRange size={15} />} loading={generating} onClick={() => handleSummary('weekly')}>生成周报</Button>
          </Tooltip>
        </div>
      </section>

      <div className="ai-center__workspace">
        <aside className="ai-history">
          <div className="ai-history__head">
            <div><strong>分析记录</strong><small>{reports.length} 条证据报告</small></div>
            <Select
              size="small"
              allowClear
              placeholder="风险"
              value={filters.riskLevel}
              onChange={(riskLevel) => setFilters((value) => ({ ...value, riskLevel }))}
              options={[
                { value: 'HIGH', label: '高风险' },
                { value: 'MEDIUM', label: '需关注' },
                { value: 'LOW', label: '低风险' },
              ]}
            />
          </div>
          <div className="ai-history__list">
            {loading ? (
              <div className="ai-history__state"><Spin /></div>
            ) : reports.length === 0 ? (
              <div className="ai-history__state"><Empty description="还没有分析报告" /></div>
            ) : reports.map((report) => {
              const SourceIcon = sourceMeta[report.sourceType]?.icon ?? FileText;
              return (
                <button
                  type="button"
                  key={report.id}
                  className={`ai-history__item ${selectedReport?.id === report.id ? 'is-active' : ''}`}
                  onClick={() => handleSelect(report.id)}
                >
                  <span className={`ai-history__risk ai-history__risk--${report.riskLevel.toLowerCase()}`} />
                  <span className="ai-history__copy">
                    <span><SourceIcon size={13} />{sourceMeta[report.sourceType]?.label ?? report.sourceType}</span>
                    <strong>{report.title}</strong>
                    <small>{report.summary}</small>
                    <time>{formatDate(report.createdAt)}</time>
                  </span>
                </button>
              );
            })}
          </div>
        </aside>

        <main className="ai-center__detail">
          {selectedReport ? <ReportDetail report={selectedReport} /> : (
            <div className="ai-center__empty">
              <BrainCircuit size={38} />
              <strong>等待第一份证据报告</strong>
              <p>从告警、检测记录或上方图片入口发起多模态分析。</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
