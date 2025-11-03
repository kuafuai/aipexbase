import {createI18n} from 'vue-i18n'
import en from './en.js'
import zh from './zh.js'

const lange = import.meta.env.VITE_APP_LANGE || 'en'

const i18n = createI18n({
    locale: lange,
    fallbackLocale: 'en',
    legacy: false,
    messages: {
        en,
        zh
    }
})

export default i18n