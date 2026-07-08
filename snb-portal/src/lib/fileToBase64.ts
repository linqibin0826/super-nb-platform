// File → base64（剥离 data URL 头）。参考图落库前用：把浏览器 File 转成后端要的纯 b64 + contentType。
function readAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = String(reader.result)
      const comma = result.indexOf(',')
      resolve(comma >= 0 ? result.slice(comma + 1) : result)
    }
    reader.onerror = () => reject(reader.error ?? new Error('FileReader failed'))
    reader.readAsDataURL(file)
  })
}

export async function filesToRefB64(files: File[]): Promise<{ b64: string; contentType: string }[]> {
  const out: { b64: string; contentType: string }[] = []
  for (const file of files) {
    out.push({ b64: await readAsBase64(file), contentType: file.type || 'image/png' })
  }
  return out
}
