// End-to-end check: two clients, one English and one Persian, exchange Persian chat.
// The English client must receive the shaped, reversed form; the Persian client the raw text.
import mineflayer from 'mineflayer'

const host = process.env.MC_HOST ?? 'localhost'
const port = Number(process.env.MC_PORT ?? 25565)
const version = process.env.MC_VERSION

const RAW = 'سلام دنیا'
const VISUAL = 'ﺎﯿﻧﺩ ﻡﻼﺳ'
// EXPECT=legacy: proxy jars rewrite the sender's message once, so every client gets the visual form.
// EXPECT=untouched: proxy jars on 1.19.1+ clients must leave signed chat alone.
const expect = process.env.EXPECT ?? 'backend'

function connect(username, locale) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({ host, port, username, version, auth: 'offline' })
    bot.received = []
    // A 1.19.1+ client displays the unsigned override when the server sends one; the signed body stays intact.
    bot.on('message', (msg) => bot.received.push((msg.unsigned ?? msg).toString()))
    if (process.env.DEBUG_PACKETS) {
      bot._client.on('player_chat', (p) => console.log(`[${username}] player_chat`, JSON.stringify(p)))
      bot._client.on('system_chat', (p) => console.log(`[${username}] system_chat`, JSON.stringify(p)))
    }
    bot.once('spawn', () => {
      bot.setSettings({ locale })
      resolve(bot)
    })
    bot.once('kicked', (reason) => reject(new Error(`${username} kicked: ${reason}`)))
    bot.once('error', reject)
  })
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function waitFor(bot, predicate, label, timeoutMs = 10000) {
  const deadline = Date.now() + timeoutMs
  while (Date.now() < deadline) {
    const hit = bot.received.find(predicate)
    if (hit) return hit
    await sleep(100)
  }
  throw new Error(`${label}: not received within ${timeoutMs}ms. Got: ${JSON.stringify(bot.received)}`)
}

let failed = 0
function check(label, ok, detail) {
  console.log(`${ok ? 'ok  ' : 'FAIL'} ${label}${ok ? '' : ` :: ${detail}`}`)
  if (!ok) failed++
}

const en = await connect('EnglishBot', 'en_us')
await sleep(4500) // bukkit.yml connection-throttle: one login per address per 4s
const fa = await connect('PersianBot', 'fa_ir')
await sleep(1500) // let the client settings packets land before chatting

for (const sender of [en, fa]) {
  en.received.length = 0
  fa.received.length = 0
  sender.chat(RAW)

  const gotEn = await waitFor(en, () => true, `${sender.username} -> en`)
  const gotFa = await waitFor(fa, () => true, `${sender.username} -> fa`)
  const visual = (t) => t.includes(VISUAL) && !t.includes(RAW)
  const raw = (t) => t.includes(RAW) && !t.includes(VISUAL)
  if (expect === 'untouched') {
    check(`${sender.username} -> English client gets raw text (signed chat, proxy must not touch it)`, raw(gotEn), gotEn)
    check(`${sender.username} -> Persian client gets raw text`, raw(gotFa), gotFa)
  } else {
    check(`${sender.username} -> English client gets visual form`, visual(gotEn), gotEn)
    if (expect === 'legacy') {
      check(`${sender.username} -> Persian client gets visual form too (legacy proxy design)`, visual(gotFa), gotFa)
    } else {
      check(`${sender.username} -> Persian client gets raw text`, raw(gotFa), gotFa)
    }
  }
}

en.quit()
fa.quit()
process.exit(failed ? 1 : 0)
