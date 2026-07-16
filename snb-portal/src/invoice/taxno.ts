/** 税号格式验真(与后端 TaxNoFormat.java 同一套规则,改必两边同步):
 *  18 位统一社会信用代码 = GB 32100-2015 字符集(无 I/O/S/V/Z)+ 末位加权 mod31 校验码;
 *  15 位纯数字 = 三证合一前的老税务登记证号,存量放行;其余拒。
 *  验的是「格式与校验位」(挡乱填/抄错一位),不验企业是否存在(那是核验接口的事)。 */
const ALPHABET = '0123456789ABCDEFGHJKLMNPQRTUWXY'
const WEIGHTS = [1, 3, 9, 27, 19, 26, 16, 17, 20, 29, 25, 13, 8, 24, 10, 30, 28]

export function isValidTaxNo(raw: string): boolean {
  const value = raw.trim().toUpperCase()
  if (value.length === 15) return /^[0-9]{15}$/.test(value)
  if (value.length !== 18) return false
  let sum = 0
  for (let i = 0; i < 17; i++) {
    const code = ALPHABET.indexOf(value[i])
    if (code < 0) return false
    sum += code * WEIGHTS[i]
  }
  return ALPHABET.indexOf(value[17]) === (31 - (sum % 31)) % 31
}
