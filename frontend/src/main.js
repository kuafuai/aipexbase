import {createApp} from 'vue'
import App from './App.vue'

// 自定义样式
import '@/styles/index.css';

const app = createApp(App);

// element plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'
const lang = import.meta.env.VITE_APP_LANGE || 'en'
const elementLocale = lang === 'zh' ? zhCn : en

app.use(ElementPlus, {
    locale: elementLocale,
});

// 注册element icon
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}

import i18n from '@/i18n'
app.use(i18n);
app.config.globalProperties.$tt = i18n.global.t;

// 路由
import router from './router'

app.use(router);

// 全局组件注册
import myComponent from '@/components/index';

Object.keys(myComponent).forEach((key) => {
    app.component(key, myComponent[key]);
});

// 配置全局api
import api from '@/api';

app.config.globalProperties.$api = api;

import {parseTime} from '@/utils/tools';

app.config.globalProperties.parseTime = parseTime

// plugins
import plugins from './plugins'

app.use(plugins)

// 权限控制
import './permission'

app.mount('#app');
