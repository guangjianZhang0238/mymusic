const globalKey = '__mymusic_global_audio__'
const holder = window as typeof window & { [globalKey]?: HTMLAudioElement }

if (!holder[globalKey]) {
  holder[globalKey] = new Audio()
}

export const playerAudio = holder[globalKey] as HTMLAudioElement
