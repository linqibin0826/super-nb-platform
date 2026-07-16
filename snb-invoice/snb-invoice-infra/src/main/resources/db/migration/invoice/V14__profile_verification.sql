-- 抬头第三方核验章:verified_at=最近一次「名称+税号与官方开票资料完全一致」的时刻(app 层
-- ProfileStamp 判定,编辑名称/税号即掉章重判);申请单同款快照列,受理端看的是提交那一刻的章。
ALTER TABLE invoice.invoice_profile ADD COLUMN verified_at TIMESTAMPTZ;
ALTER TABLE invoice.invoice_request ADD COLUMN profile_verified_at TIMESTAMPTZ;
