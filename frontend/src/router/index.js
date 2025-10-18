import {createRouter, createWebHistory} from 'vue-router'

export const constantRouters = [
    {
        path: '/login',
        component: () => import('@/views/login/index.vue'),
    },
    {
        path: '/register',
        component: () => import('@/views/register/index.vue'),
    },
    {
        path: "/",
        component: () => import("@/layout/Layout.vue"),
        children: [
            {
                path: '',
                redirect: '/dashboard'
            },
            {
                path: 'dashboard',
                component: () => import("@/views/dashboard/index.vue")
            },
            {
                path: 'project/:id',
                component: () => import('@/views/project/index.vue'),
                name: 'ProjectDetail',
                redirect: {name: 'ProjectOverview'},
                props: true,
                children: [
                    {
                        path: 'overview',
                        name: 'ProjectOverview',
                        component: () => import('@/views/project/overview.vue'),
                    },
                    {
                        path: 'tables',
                        name: 'ProjectTables',
                        component: () => import('@/views/project/tables.vue'),
                    },
                    {
                        path: 'settings',
                        name: 'ProjectSettings',
                        component: () => import('@/views/project/settings.vue'),
                        redirect: {name: 'ProjectSettingsBasic'},
                        props: true,
                        children: [
                            {
                                path: 'basic',
                                name: 'ProjectSettingsBasic',
                                component: () => import('@/views/settings/basic.vue'),
                            },
                            {
                                path: 'auth',
                                name: 'ProjectSettingsAuth',
                                component: () => import('@/views/settings/auth.vue'),
                            },
                            {
                                path: 'permission',
                                name: 'ProjectSettingsPermission',
                                component: () => import('@/views/settings/permission.vue'),
                            },
                            {
                                path: 'cleanup',
                                name: 'ProjectSettingsCleanup',
                                component: () => import('@/views/settings/cleanup.vue'),
                            },
                            {
                                path: 'delete',
                                name: 'ProjectSettingsDelete',
                                component: () => import('@/views/settings/delete.vue'),
                            },
                            {
                                path: 'users',
                                name: 'ProjectSettingsUsers',
                                component: () => import('@/views/settings/users.vue'),
                            },
                        ]
                    },
                    {
                        path: 'apikeys',
                        name: 'ProjectApiKeys',
                        component: () => import('@/views/project/apikeys.vue'),
                    },
                    {
                        path: 'dynamicapi',
                        name: 'dynamicApi',
                        component: () => import('@/views/project/dynamicapi.vue'),
                    }
                ]
            },
        ]
    },
    {
        path: "/:pathMatch(.*)*",
        component: () => import('@/views/error/404.vue'),
        hidden: true
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.VITE_BASE),
    routes: constantRouters,
});

export default router;