const modulesFiles = import.meta.glob('./*/*.vue', {import: 'default', eager: true});

const modules = {};
for (const key in modulesFiles) {
  const moduleName = key.replace(/(.*\/)*([^.]+).*/gi, '$2');
  modules[moduleName] = modulesFiles[key];
}

export default modules;