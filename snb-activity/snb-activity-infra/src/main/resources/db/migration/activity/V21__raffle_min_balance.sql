-- 余额闸(2026-08-18 站长拍板,第四期起):资格 = 充值闸 或 站内余额 ≥ min_balance 任一满足。
-- min_balance 可空 = 该期不启用余额闸(存量期与不填的新期语义完全不变)。
-- balance_at_entry = 报名时点余额快照:站长拍板「报名时够就行」——开奖复核走快照,
-- 报名后正常消耗不影响资格(充值闸复核仍走实时值,堵退款套利,两闸语义各自独立)。
ALTER TABLE activity.raffle_campaign ADD COLUMN min_balance numeric(12,2);
ALTER TABLE activity.raffle_entry ADD COLUMN balance_at_entry numeric(12,2);
