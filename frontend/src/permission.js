import router from '@/router';

import {dynamicRoutes} from '@/router/dynamic'

import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

NProgress.configure({showSpinner: true});

let hasRouter = false;
const useLogin = import.meta.env.VITE_USE_LOGIN === 'true'
const whiteList = ['/login', '/register'];

router.beforeEach(async (to, from, next) => {
    NProgress.start();
    if (useLogin) {
        // 判断有没有登录
        if (localStorage.getItem("token")) {
            //登录成功
            if (to.path === '/login') {
                if (to.fullPath.startsWith('/login?redirect=')) {
                    let lastPath = to.fullPath.replace('/login?redirect=', '');
                    next({path: lastPath}); // 跳转到上次退出的页面
                } else {
                    next({path: '/'}); // 跳转到首页
                }
            } else {
                if (hasRouter) {
                    next(); // 放行
                } else {
                    dynamicRoutes.forEach((e) => router.addRoute(e));
                    hasRouter = true;
                    next({...to, replace: true});
                }
            }
        } else {
            if (whiteList.indexOf(to.path) !== -1) {
                next(); // 放行 -- 可以访问白名单页面(eg: 登录页面)
            } else {
                next(`/login`);
            }
        }
    } else {
        if (hasRouter) {
            next(); // 放行
        } else {
            dynamicRoutes.forEach((e) => router.addRoute(e));
            hasRouter = true;
            next({...to, replace: true});
        }
    }
});

// 全局后置钩子
router.afterEach(() => {
    NProgress.done();
});
