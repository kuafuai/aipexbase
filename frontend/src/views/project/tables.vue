<template>
  <div
      class="min-h-screen bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 text-white flex relative overflow-hidden">

    <div
        class="absolute inset-0 bg-[radial-gradient(ellipse_at_left,_var(--tw-gradient-stops))] from-cyan-500/5 via-transparent to-transparent"></div>


    <aside class="relative z-10 w-80 bg-white/5 backdrop-blur-xl border-r border-white/10 p-6 flex flex-col h-screen">

      <div class="flex items-center space-x-3 mb-6">
        <div
            class="w-10 h-10 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-xl flex items-center justify-center shadow-lg shadow-cyan-500/25">

          <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
          </svg>
        </div>

        <div>
          <h2 class="text-xl font-bold bg-gradient-to-r from-cyan-400 to-blue-400 bg-clip-text text-transparent">
            {{ t('page.tables.title') }}
          </h2>
          <p class="text-sm text-white/60">{{ t('page.tables.sub') }}</p>
        </div>

      </div>

      <el-button
          type="primary"
          size="large"
          class="w-full bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25 mb-6"
          @click="newTableClick"
      >
        <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
        </svg>
        {{ t('page.tables.new_table') }}
      </el-button>

      <div class="flex-1 overflow-hidden flex flex-col min-h-0">
        <el-scrollbar class="h-full">
          <div class="space-y-2 pr-2">
            <div
                v-for="table in tables"
                :key="table.tableName"
                class="group relative p-4 rounded-xl cursor-pointer transition-all duration-300 border"
                :class="{
                'bg-gradient-to-r from-cyan-500/20 to-blue-500/20 border-cyan-500/30 text-cyan-300 shadow-lg shadow-cyan-500/10': table.tableName === currentTable?.tableName,
                'bg-white/5 border-white/10 hover:bg-cyan-500/10 hover:border-cyan-400/30 hover:text-cyan-200': table.tableName !== currentTable?.tableName
              }"
                @click="selectTable(table)"
            >

              <div
                  v-if="table.tableName === currentTable?.tableName"
                  class="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-8 bg-gradient-to-b from-cyan-400 to-blue-400 rounded-r-full"
              ></div>
              <div class="flex items-center justify-between">
                <div class="flex items-center space-x-3 flex-1 min-w-0">

                  <div
                      class="w-8 h-8 rounded-lg flex items-center justify-center transition-all duration-300"
                      :class="{
                    'bg-gradient-to-br from-cyan-500 to-blue-500 text-white shadow-md': table.tableName === currentTable?.tableName,
                    'bg-white/5 text-white/50 group-hover:bg-cyan-500/10 group-hover:text-cyan-300': table.tableName !== currentTable?.tableName
                  }"
                  >

                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"/>
                    </svg>
                  </div>

                  <div class="flex-1 min-w-0">
                    <div class="font-medium text-sm truncate group-hover:text-cyan-200 transition-colors duration-200">
                      {{ table.description }}
                    </div>

                    <div
                        class="text-xs mt-1 truncate transition-colors duration-200"
                        :class="{
                      'text-cyan-100/80': table.tableName === currentTable?.tableName,
                      'text-white/50 group-hover:text-cyan-100': table.tableName !== currentTable?.tableName
                    }"
                    >
                      {{ table.tableName }}
                    </div>

                  </div>
                </div>
                <div class="ml-3" @click.stop>
                  <el-dropdown
                      trigger="click"
                      placement="bottom-end"
                      :hide-on-click="false"
                      @command="handleTableCommand"
                  >
                    <el-button
                        type="text"
                        size="small"
                        circle
                        class="w-6 h-6 !min-w-6 opacity-100 transition-all duration-300 hover:bg-white/10"
                    >
                      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                              d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"/>
                      </svg>
                    </el-button>

                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item :command="{ action: 'edit', table: table }">
                          <div class="flex items-center space-x-2">
                            <div class="w-4 h-4 bg-cyan-500/20 rounded flex items-center justify-center">
                              <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
                              </svg>
                            </div>
                          <span>{{ t('page.tables.edit_table') }}</span>
                          </div>
                        </el-dropdown-item>
                        <el-dropdown-item :command="{ action: 'delete', table: table }">
                          <div class="flex items-center space-x-2">
                            <div class="w-4 h-4 bg-red-500/20 rounded flex items-center justify-center">
                              <svg class="w-3 h-3 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                                      d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                              </svg>
                            </div>
                            <span>{{ t('page.tables.delete_table') }}</span>
                          </div>
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>

              </div>
            </div>

            <div v-if="tables.length === 0" class="text-center py-12">
              <div class="w-16 h-16 mx-auto mb-4 bg-white/5 rounded-2xl flex items-center justify-center">
                <svg class="w-8 h-8 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
                        d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
                </svg>
              </div>

              <p class="text-white/60 text-sm">{{ t('page.tables.no_tables') }}</p>
              <p class="text-white/40 text-xs mt-1">{{ t('page.tables.no_tables_tip') }}</p>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </aside>

    <main class="relative z-10 flex-1 flex flex-col min-w-0">
      <div v-if="currentTable" class="bg-white/5 backdrop-blur-xl border-b border-white/10 p-6">
        <div class="flex items-center justify-between">
          <div class="flex items-center space-x-4">
            <div
                class="w-12 h-12 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-xl flex items-center justify-center shadow-lg shadow-cyan-500/25">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M3 10h18M3 14h18m-9-4v8m-7 0h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
              </svg>
            </div>

            <div>
              <h1 class="text-2xl font-bold text-white">{{ currentTable.description }}</h1>
              <p class="text-white/60 mt-1 flex items-center space-x-2">
                <span>{{ currentTable.tableName }}</span>
                <span class="text-white/40">•</span>
                <span class="flex items-center space-x-1 text-sm">
                  <div class="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                  <span>{{ t('page.layout.running') }}</span>
                </span>
              </p>
            </div>
          </div>

          <div class="flex items-center space-x-3">
            <el-button
                type="primary"
                size="large"
                class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
                @click="exportData"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"/>
              </svg>
              {{ t('page.tables.export_data') }}
            </el-button>
            <el-button
                type="primary"
                size="large"
                class="bg-gradient-to-r from-green-500 to-emerald-500 border-0 text-white hover:from-green-600 hover:to-emerald-600 transition-all duration-300 shadow-lg shadow-green-500/25"
                @click="addData"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              {{ t('page.tables.add_data') }}
            </el-button>
            <!-- 下载模板按钮 -->
            <el-button
                type="primary"
                size="large"
                class="bg-gradient-to-r from-amber-500 to-orange-500 border-0 text-white hover:from-amber-600 hover:to-orange-600 transition-all duration-300 shadow-lg shadow-amber-500/25"
                @click="downloadTemplate"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
              </svg>
              {{ t('page.tables.download_template') }}
            </el-button>
            <!-- 导入按钮 -->
            <el-button
                type="primary"
                size="large"
                class="bg-gradient-to-r from-purple-500 to-indigo-500 border-0 text-white hover:from-purple-600 hover:to-indigo-600 transition-all duration-300 shadow-lg shadow-purple-500/25"
                @click="showImportDialog"
            >
              <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M9 19l3 3m0 0l3-3m-3 3V10"/>
              </svg>
              {{ t('page.tables.import_data') }}
            </el-button>
          </div>

        </div>
      </div>

      <div v-if="currentTable" class="flex-1 p-1 overflow-auto">
        <div class="bg-white/5 backdrop-blur-md rounded-2xl border border-white/10 overflow-hidden ">

          <el-table
              :data="pageRes.records"
              v-loading="tableLoading"
              stripe
              border
              class="custom-table"
              style="width: 100%;"
              :empty-text="t('page.tables.empty_text')"
          >

            <template v-for="column in tableColumns" :key="column.columnName">
              <el-table-column
                  v-if="column.dslType === 'images' || column.dslType === 'image'"
                  :prop="column.columnName"
                  :label="column.columnComment"
                  min-width="180"
                  align="center"
                  show-overflow-tooltip
              >
                <template #default="scope">
                  <image-preview
                      :width="50"
                      :height="50"
                      :src="getImageUrl(scope.row[column.columnName])"
                      class="rounded-lg shadow-md"
                  />
                </template>
              </el-table-column>

              <el-table-column
                  v-else-if="column.dslType === 'quote'"
                  :prop="column.columnName"
                  :label="column.columnComment"
                  min-width="180"
                  align="center"
                  show-overflow-tooltip
              >
                <template #default="scope">
                  <span class="text-cyan-400 font-medium">
                    {{ get_show_name(scope.row, column.columnName, column.showName) }}
                  </span>
                </template>
              </el-table-column>

              <el-table-column
                  v-else-if="column.dslType === 'boolean'"
                  :prop="column.columnName"
                  :label="column.columnComment"
                  min-width="180"
                  align="center"
                  show-overflow-tooltip
              >
                <template #default="scope">
                  <el-tag
                      :type="scope.row[column.columnName] ? 'success' : 'info'"
                      effect="dark"
                      class="rounded-full px-3"
                  >
                    {{ scope.row[column.columnName] ? t('page.tables.bool_true') : t('page.tables.bool_false') }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column
                  v-else-if="column.dslType === 'password'"
                  :prop="column.columnName"
                  :label="column.columnComment"
                  min-width="180"
                  align="center"
                  show-overflow-tooltip
              >
                <template #default="scope">
                  <div class="flex items-center space-x-2">
                    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                            d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"/>
                    </svg>
                    <span>••••••</span>
                  </div>
                </template>
              </el-table-column>

              <el-table-column
                  v-else
                  :prop="column.columnName"
                  :label="column.columnComment"
                  min-width="180"
                  align="center"
                  show-overflow-tooltip
              >
                <template #default="scope">
                  {{ scope.row[column.columnName] }}
                </template>
              </el-table-column>


            </template>


            <el-table-column fixed="right" :label="t('page.tables.operations')" width="140" align="center">
              <template #default="scope">
                <div class="flex items-center justify-center space-x-2">
                  <el-button
                      type="primary"
                      circle
                      size="small"
                      class="bg-cyan-500 border-0 hover:bg-cyan-600 transition-all duration-300 shadow-md shadow-cyan-500/25"
                      @click="updateData(scope.row)"
                  >
                    <el-icon>
                      <Edit/>
                    </el-icon>
                  </el-button>
                  <el-button
                      type="danger"
                      circle
                      size="small"
                      class="bg-red-500 border-0 hover:bg-red-600 transition-all duration-300 shadow-md shadow-red-500/25"
                      @click="deleteData(scope.row)"
                  >
                    <el-icon>
                      <Delete/>
                    </el-icon>
                  </el-button>
                </div>
              </template>
            </el-table-column>

          </el-table>

        </div>

        <div class="mt-6 flex justify-center" v-if="pageRes.pages > 1">
          <el-pagination
              background
              layout="prev, pager, next"
              :page-size="pageParams.pageSize"
              :current-page.sync="pageParams.current"
              :total="pageRes.total"
              @current-change="handlePageChange"
              class="custom-pagination"
          />
        </div>
      </div>

      <div v-else class="flex-1 flex items-center justify-center">
        <div class="text-center">
          <div class="w-24 h-24 mx-auto mb-6 bg-white/5 rounded-3xl flex items-center justify-center">
            <svg class="w-12 h-12 text-white/30" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
          </div>
      <h3 class="text-xl font-semibold text-white/80 mb-2">{{ t('page.tables.select_table_title') }}</h3>
      <p class="text-white/60">{{ t('page.tables.select_table_tip') }}</p>
        </div>
      </div>

    </main>

  </div>


  <el-drawer
      v-model="drawerAddAndEdit"
      :title="t('page.tables.data_manage')"
      direction="rtl"
      size="480px"
      class="custom-drawer"
  >
    <template #header>
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-lg flex items-center justify-center">
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
          </svg>
        </div>
        <h4 class="text-lg font-semibold ">
          {{ dataMode === 'edit' ? t('page.tables.edit_data') : t('page.tables.add_data') }}
        </h4>
      </div>
    </template>

    <div class="p-6">
      <el-form ref="formRef" :model="dataForm" label-width="auto" label-position="top" class="space-y-6">

        <template v-for="column in filteredColumns" :key="column.columnName">

          <el-form-item v-if="column.dslType === 'password'"
                        :label="column.columnComment"
                        :prop="column.columnName"
                        :rules="getFieldRules(column)">
            <el-input v-model="dataForm[column.columnName]"
                      type="password"
                      show-password
                      size="large"
                      class="custom-input"
                      :placeholder="t('page.tables.input_password')"
            />
          </el-form-item>

          <el-form-item v-else-if="column.dslType === 'quote'"
                        :label="column.columnComment"
                        :prop="column.columnName"
                        :rules="getFieldRules(column)">

            <el-select
                v-model="dataForm[column.columnName]"
                class="custom-input"
                size="large"
                :placeholder="t('page.tables.please_select', { label: column.columnComment })"
                filterable
                remote
                :remote-method="table_select_data(column.referenceTableName, column.columnName)"
            >
              <el-option
                  v-for="item in selectOptions[column.columnName]"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
              >

              </el-option>
            </el-select>

          </el-form-item>

          <el-form-item v-else-if="column.dslType === 'images' || column.dslType === 'image'"
                        :label="column.columnComment"
                        :prop="column.columnName"
                        :rules="getFieldRules(column)">
            <image-upload v-model="dataForm[column.columnName]" :type="true"/>
          </el-form-item>

          <el-form-item v-else-if="column.dslType === 'date' "
                        :label="column.columnComment"
                        :prop="column.columnName"
                        :rules="getFieldRules(column)">
            <el-date-picker v-model="dataForm[column.columnName]"
                            type="date"
                            placeholder="默认值,now()"
                            format="YYYY-MM-DD"
                            value-format="YYYY-MM-DD"
                            size="large"
                            class="w-full custom-input"/>
          </el-form-item>

          <el-form-item v-else
                        :label="column.columnComment"
                        :prop="column.columnName"
                        :rules="getFieldRules(column)">
            <el-input v-model="dataForm[column.columnName]"
                      size="large"
                      class="custom-input"
                      :placeholder="`${t('page.tables.default_value')}-${column.columnComment}`"
            />
          </el-form-item>
        </template>
      </el-form>
    </div>

    <template #footer>
      <div class="flex justify-end space-x-3 px-6 pb-6">
        <el-button
            @click="drawerAddAndEdit = false"
            class="border-white/20 text-white/80 hover:bg-white/5 transition-all duration-300"
        >
          {{ t('page.project.cancel') }}
        </el-button>
        <el-button
            type="primary"
            @click="onSaveData"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
        >
          {{ t('page.tables.save_data') }}
        </el-button>
      </div>
    </template>

  </el-drawer>


  <el-drawer
      v-model="drawerTable"
      direction="rtl"
      size="520px"
      class="custom-drawer"
  >
    <template #header>
      <div class="flex items-center space-x-3">
        <div class="w-8 h-8 bg-gradient-to-br from-cyan-500 to-blue-500 rounded-lg flex items-center justify-center">
          <svg class="w-4 h-4 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/>
          </svg>
        </div>
        <h4 class="text-lg font-semibold">
          {{ drawerTableMode === 'edit' ? t('page.tables.drawer_table_title_edit') : t('page.tables.drawer_table_title_add') }}
        </h4>
      </div>
    </template>

    <div class="p-6">

      <el-form ref="tableFormRef" :model="tableForm" :rules="tableRules" label-position="top" class="space-y-6">
        <!-- 表基本信息 -->
        <div class=" rounded-xl p-4 border border-cyan/10">
          <div class="flex items-center space-x-2 mb-4">
            <div class="w-2 h-2 bg-cyan-400 rounded-full"></div>
            <h5 class="text-sm font-semibold text-cyan-300">{{ t('page.tables.table_basic') }}</h5>
          </div>

          <el-form-item :label="t('page.tables.table_name')" prop="tableName" class="mb-0">
            <el-input
                v-model="tableForm.tableName"
                :placeholder="t('page.tables.table_name_placeholder')"
                size="large"
                :disabled="drawerTableMode === 'edit'"
                class="custom-input"
            />
            <div class="text-xs text-black mt-1">{{ t('page.tables.table_name_hint') }}</div>
          </el-form-item>

          <el-form-item :label="t('page.tables.table_desc')" prop="description" class="mb-0 mt-4">
            <el-input
                v-model="tableForm.description"
                :placeholder="t('page.tables.table_desc')"
                size="large"
                class="custom-input"
            />
          </el-form-item>
        </div>

        <!-- 字段列表 -->
        <div class=" rounded-xl p-4 border border-cyan/10">
          <div class="flex items-center justify-between mb-4">
            <div class="flex items-center space-x-2">
              <div class="w-2 h-2 bg-blue-400 rounded-full"></div>
              <h5 class="text-sm font-semibold text-blue-300">{{ t('page.tables.field_list') }}</h5>
            </div>
            <el-button
                type="primary"
                size="small"
                @click="addField"
                class="bg-gradient-to-r from-blue-500 to-indigo-500 border-0"
            >
              <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
              </svg>
              {{ t('page.tables.add_field') }}
            </el-button>
          </div>

          <div v-if="tableForm.columns.length === 0" class="text-center py-8 bg-gray-400">
            <svg class="w-12 h-12 mx-auto mb-3 " fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1"
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/>
            </svg>
            <p class="text-sm">{{ t('page.tables.no_fields') }}</p>
          </div>

          <div v-else class="space-y-4 max-h-screen overflow-y-auto pr-2">
            <div
                v-for="(field, index) in tableForm.columns"
                :key="index"
                class="bg-white/3 rounded-lg p-4 border border-white/10 hover:border-blue-400/30 transition-all duration-300"
            >
              <div class="flex items-center justify-between mb-3">
                <div class="flex items-center space-x-2">
                  <div class="w-6 h-6 bg-blue-500/20 rounded flex items-center justify-center">
                    <span class="text-xs font-medium text-blue-400">{{ index + 1 }}</span>
                  </div>
                  <span class="text-sm font-medium text-white">{{ t('page.tables.field_label', { index: index + 1 }) }}</span>
                </div>
                <el-button
                    type="danger"
                    size="small"
                    circle
                    @click="removeField(index)"
                    class="bg-red-500/20 border-red-500/30 text-red-400 hover:bg-red-500/30"
                >
                  <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </el-button>
              </div>

              <div class="grid grid-cols-2 gap-3">
                <el-form-item :label="t('page.tables.field_name')"
                              :prop="'columns.' + index + '.columnName'"
                              :rules="fieldRules.columnName">
                  <el-input
                      v-model="field.columnName"
                      :placeholder="t('page.tables.field_name_placeholder')"
                      size="small"
                      class="custom-input"
                  />
                </el-form-item>

                <el-form-item :label="t('page.tables.field_type')"
                              :prop="'columns.' + index + '.columnType'"
                              :rules="fieldRules.columnType">
                  <el-select
                      v-model="field.columnType"
                      :placeholder="t('page.tables.choose_type')"
                      size="small"
                      class="custom-input w-full"
                  >
                    <el-option
                        v-for="type in fieldTypes"
                        :key="type.value"
                        :label="type.label"
                        :value="type.value"
                    />
                  </el-select>
                </el-form-item>
              </div>

              <el-form-item v-if="showQuote(field.columnType)" :label="t('page.tables.ref_object')" class="mb-0 mt-3">
                <el-select
                    v-model="field.referenceTableName"
                    :options="tables"
                    :props="props"
                    class="custom-input"
                    size="small"
                    :placeholder="t('page.tables.ref_object')"
                />
              </el-form-item>

              <el-form-item :label="t('page.tables.field_desc')"
                            :prop="'columns.' + index + '.columnComment'"
                            :rules="fieldRules.columnComment"
                            class="mb-0">
                <el-input
                    v-model="field.columnComment"
                    :placeholder="t('page.tables.field_desc')"
                    size="small"
                    class="custom-input"
                />
              </el-form-item>

              <!-- 字段额外配置 -->
              <div class="grid grid-cols-2 gap-3 mt-3">
                <el-form-item :label="t('page.tables.is_primary')" class="mb-0">
                  <el-switch
                      v-model="field.isPrimary"
                      active-color="#06b6d4"
                      class="custom-switch"
                  />
                </el-form-item>

                <el-form-item :label="t('page.tables.is_required')" class="mb-0">
                  <el-switch
                      v-model="field.isNullable"
                      active-color="#06b6d4"
                      class="custom-switch"
                  />
                </el-form-item>

                <el-form-item :label="t('page.tables.is_show')" class="mb-0">
                  <el-switch
                      v-model="field.isShow"
                      active-color="#06b6d4"
                      class="custom-switch"
                  />
                </el-form-item>

              </div>

              <!-- 默认值设置 -->
              <el-form-item v-if="showDefaultValue(field.columnType)" :label="t('page.tables.default_value')" class="mb-0 mt-3">
                <el-input
                    v-model="field.defaultValue"
                    :placeholder="getDefaultValuePlaceholder(field.columnType)"
                    size="small"
                    class="custom-input"
                />
              </el-form-item>
            </div>
          </div>
        </div>
      </el-form>

    </div>


    <template #footer>
      <div class="flex justify-end space-x-3 px-6 pb-6">
        <el-button
            @click="drawerTable = false"
            class="border-white/20 text-white/80 hover:bg-white/5 transition-all duration-300"
        >
          {{ t('page.project.cancel') }}
        </el-button>
        <el-button
            type="primary"
            :loading="savingTable"
            @click="onSaveTable"
            class="bg-gradient-to-r from-cyan-500 to-blue-500 border-0 text-white hover:from-cyan-600 hover:to-blue-600 transition-all duration-300 shadow-lg shadow-cyan-500/25"
        >
          <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
          </svg>
          {{ t('page.tables.save') }}
        </el-button>
      </div>
    </template>

  </el-drawer>
  <!-- 放在模板最下面，和抽屉平级 -->
  <el-dialog
      v-model="importDialogVisible"
      :title="t('page.tables.import_data')"
      width="420px"
      top="15vh"
      class="custom-dialog">
    <div class="space-y-4">
      <el-upload
          drag
          :limit="1"
          accept=".xlsx,.xls"
          :before-upload="beforeUpload"
          :on-change="handleFileChange"
          :on-remove="clearSelectedFile"
          :auto-upload="false">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          {{ t('page.tables.drop_file') }}
        </div>
      </el-upload>
    </div>

    <template #footer>
      <div class="flex justify-end space-x-3">
        <el-button @click="importDialogVisible = false">
          {{ t('page.project.cancel') }}
        </el-button>
        <el-button
            type="primary"
            :loading="importLoading"
            @click="confirmImport">
          {{ t('page.tables.import_data') }}
        </el-button>
      </div>
    </template>
  </el-dialog>

</template>

<script setup>

const {proxy} = getCurrentInstance();
const t = proxy.$tt;
const appId = proxy.$route.params.id;
const tables = ref([])

const currentTable = ref({})
const tableColumns = ref([]);

const tableLoading = ref(false)
const pageParams = ref({current: 1, pageSize: 10});
const pageRes = ref({current: 1, pages: 1, size: 10, total: 0, records: []});

const drawerAddAndEdit = ref(false);
const dataForm = ref({});
const dataMode = ref('add');
const selectOptions = ref({});

const drawerTable = ref(false);
const drawerTableMode = ref('add')
const savingTable = ref(false)
const tableForm = ref({
  tableName: '',
  description: '',
  columns: []
});

// 新增导入相关的响应式数据
const importDialogVisible = ref(false);
const selectedFile = ref(null);
const importLoading = ref(false);

const props = {
  value: 'tableName',
  label: 'description',
  options: 'options',
  disabled: 'disabled',
}

onMounted(() => {
  loadTables(true);
})

function loadTables(select) {
  proxy.$api.dataset.tables({appId: appId}).then((res) => {
    tables.value = res.data || [];
    if (select) {
      if (tables.value.length > 0) {
        selectTable(tables.value[0]);
      } else {
        currentTable.value = null;
      }
    }
  });
}

async function selectTable(table) {
  selectOptions.value = {};
  tableColumns.value = [];
  pageRes.value = {
    current: 1,
    pages: 1,
    size: 10,
    total: 0,
    records: [],
  };

  currentTable.value = table;
  selectTableColumns(table);
}

function selectTableColumns(table) {
  proxy.$api.dataset.columns({appId: appId, tableId: table.id, tableName: table.tableName}).then((res) => {
    tableColumns.value = res.data;
    // 查询出列，在去查询数据
    selectTableData(table);
  })
}

async function selectTableData(table) {
  tableLoading.value = true;
  let data = {appId: appId, tableName: table.tableName, ...pageParams.value};
  proxy.$api.dataset.datas(data).then((res) => {
    pageRes.value = res.data;
  }).finally(() => {
    tableLoading.value = false;
  })
}

function handlePageChange(val) {
  pageParams.value.current = val;
  selectTableData(currentTable.value);
}

const getImageUrl = (row) => {
  if (!row) return null;
  return row.map(item => item.url).join(',');
}

const get_show_name = (row, column, show) => {
  if (!row) return "";

  if (column === show) {
    return row[column];
  } else {
    let showName = column + "_map";
    let data = row[showName]

    if (!data) return "";
    return data[show];
  }
}

const filteredColumns = computed(() => {
  return tableColumns.value.filter(column => {
    // 排除主键
    return !column.isPrimary;
  })
});

function table_select_data(table_name, columnName) {
  if (selectOptions.value[columnName]) return;
  if (!table_name) return;
  proxy.$api.dataset.selectTableData({appId: appId, tableName: table_name}).then((res) => {
    if (res.success) {
      selectOptions.value[columnName] = res.data;
    } else {
      selectOptions.value[columnName] = [];
    }
  });
}


function deleteData(row) {
  proxy.$modal.confirm(t('page.tables.confirm_delete_data') || '您确定删除这条数据吗？').then(() => {
    let data = {appId: currentTable.value.appId, tableName: currentTable.value.tableName, data: row}
    proxy.$api.dataset.deleteData(data).then((res) => {
      if (res.success) {
        selectTableData(currentTable.value);
      }
    })
  })
}

async function exportData() {
  if (currentTable.value) {
    await proxy.$api.dataset.export({}, `${new Date().getTime()}.xlsx`,
        currentTable.value.tableName, currentTable.value.appId);
  }
}

function addData() {
  dataForm.value = {};
  dataMode.value = 'add';
  drawerAddAndEdit.value = true;
}

function updateData(row) {
  dataForm.value = {...row};
  dataMode.value = 'edit';
  drawerAddAndEdit.value = true;
}

function onSaveData() {
  let data = {appId: currentTable.value.appId, tableName: currentTable.value.tableName, data: dataForm.value}
  proxy.$refs.formRef.validate().then((r) => {
    if (dataMode.value === 'add') {
      proxy.$api.dataset.addData(data).then((res) => {
        if (res.success) {
          drawerAddAndEdit.value = false;
          selectTableData(currentTable.value);
        }
      })
    } else {
      proxy.$api.dataset.updateData(data).then((res) => {
        if (res.success) {
          drawerAddAndEdit.value = false;
          selectTableData(currentTable.value);
        }
      })
    }
  })
}

const getFieldRules = (column) => {
  const rules = []
  // 必填验证
  if (column.isNullable === true) {
    rules.push({
      required: true,
      message: `${column.columnComment}不能为空`,
      trigger: 'blur'
    })
  }

  // 根据数据类型添加其他验证
  if (column.dslType === 'number') {
    rules.push({
      type: 'number',
      message: `${column.columnComment}必须为数字`,
      trigger: 'blur'
    })
  } else if (column.dslType === 'phone') {
    rules.push({
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号码',
      trigger: 'blur'
    })
  }

  return rules
}


const tableRules = {
  tableName: [
    {required: true, message: '请输入表名', trigger: 'blur'},
    {pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '表名只能包含字母、数字和下划线，且不能以数字开头', trigger: 'blur'}
  ],
  description: [
    {required: true, message: '请输入表描述', trigger: 'blur'}
  ]
}

const fieldRules = {
  columnName: [
    {required: true, message: '请输入字段名称', trigger: 'blur'},
    {pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '字段名只能包含字母、数字和下划线，且不能以数字开头', trigger: 'blur'}
  ],
  columnType: [
    {required: true, message: '请选择字段类型', trigger: 'change'}
  ],
  columnComment: [
    {required: true, message: '请输入字段描述', trigger: 'blur'}
  ]
}

const fieldTypes = [
  {label: '字符串', value: 'string'},
  {label: '整数', value: 'number'},
  {label: '文本', value: 'text'},
  {label: '小数', value: 'decimal'},
  {label: '布尔值', value: 'boolean'},
  {label: '日期', value: 'date'},
  {label: '时间', value: 'datetime'},
  {label: '图片', value: 'images'},
  {label: '文件', value: 'files'},
  {label: '视频', value: 'videos'},
  {label: '密码', value: 'password'},
  {label: '手机', value: 'phone'},
  {label: '邮箱', value: 'email'},
  {label: '关联对象', value: 'quote'},
]

function handleTableCommand(command) {
  const {action, table} = command

  switch (action) {
    case 'edit':
      getTableInfo(table);
      break
    case 'delete':
      deleteTable(table);
      break
  }
}

function getTableInfo(table) {
  drawerTable.value = true;
  drawerTableMode.value = 'edit';
  tableForm.value = {
    tableName: '',
    description: '',
    columns: []
  };
  proxy.$api.dataset.getTableInfo({appId: table.appId, tableName: table.tableName}).then((res) => {
    if (res.success) {
      tableForm.value = res.data;
    }
  });
}

function deleteTable(table) {
  proxy.$modal.confirm(t('page.tables.confirm_delete_table', { name: table.description })).then(() => {
    let data = {appId: table.appId, id: table.id};
    proxy.$api.dataset.deleteTable(data).then((res) => {
      if (res.success) {
        proxy.$modal.msgSuccess(t('page.tables.delete_success'));
        loadTables(false);
      } else {
        proxy.$modal.msgSuccess(t('page.tables.delete_failed'));
      }
    })
  });
}

function newTableClick() {
  drawerTable.value = true;
  drawerTableMode.value = 'add';
  tableForm.value = {
    tableName: '',
    description: '',
    columns: []
  };
}

async function onSaveTable() {
  try {
    await proxy.$refs.tableFormRef.validate();
    savingTable.value = true

    const requestData = {
      appId: appId,
      ...tableForm.value
    };

    const apiCall = drawerTableMode.value === 'add'
        ? proxy.$api.dataset.createTable
        : proxy.$api.dataset.updateTable;

    const result = await apiCall(requestData);

    if (result.success) {
      const successMessage = drawerTableMode.value === 'add' ? t('page.tables.table_created') : t('page.tables.table_updated');
      proxy.$modal.msgSuccess(successMessage)
      drawerTable.value = false
      loadTables(false) // 刷新表列表
    }
  } catch (error) {
    console.error('表单验证失败:', error)
  } finally {
    savingTable.value = false
  }
}

function addField() {
  tableForm.value.columns.push(
      {
        columnName: "",
        columnType: "number",
        columnComment: "",
        isNullable: false,
        isPrimary: false,
        isShow: false,
        referenceTableName: ''
      }
  );
}

function removeField(index) {
  tableForm.value.columns.splice(index, 1)
}

function showQuote(type) {
  const noDefaultTypes = ['quote']
  return noDefaultTypes.includes(type)
}

function showDefaultValue(type) {
  const noDefaultTypes = ['text', 'json', 'images', 'files']
  return !noDefaultTypes.includes(type)
}

// 获取默认值占位符
function getDefaultValuePlaceholder(type) {
  const placeholders = {
    string: t('page.tables.default_value_placeholder.string'),
    integer: t('page.tables.default_value_placeholder.integer'),
    decimal: t('page.tables.default_value_placeholder.decimal'),
    boolean: t('page.tables.default_value_placeholder.boolean'),
    date: t('page.tables.default_value_placeholder.date'),
    datetime: t('page.tables.default_value_placeholder.datetime'),
    password: t('page.tables.default_value_placeholder.password')
  }
  return placeholders[type] || t('page.tables.default_value')
}

// 新增导入相关的方法
function showImportDialog() {
  importDialogVisible.value = true;
  selectedFile.value = null;
}

function downloadTemplate() {
  if (currentTable.value) {
    const appIdForTemplate = currentTable.value.appId || appId;
    proxy.$api.dataset.downloadImportTemplate(currentTable.value.tableName, appIdForTemplate)
        .then((response) => {
          const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
          saveAs(blob, `${currentTable.value.description}_模板.xlsx`);
        })
        .catch(error => {
          console.error('下载模板失败:', error);
          proxy.$modal.msgError('下载模板失败');
        });
  }
}


function handleFileChange(file) {
  selectedFile.value = file.raw;
}

function beforeUpload(file) {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' || 
                  file.type === 'application/vnd.ms-excel';
  const isLt5M = file.size / 1024 / 1024 < 5;

  if (!isExcel) {
    proxy.$modal.msgError('上传文件只能是 Excel 格式!');
    return false;
  }
  if (!isLt5M) {
    proxy.$modal.msgError('上传文件大小不能超过 5MB!');
    return false;
  }
  
  return true;
}

function clearSelectedFile() {
  selectedFile.value = null;
}

async function confirmImport() {
  if (!selectedFile.value) {
    proxy.$modal.msgError('请先选择要导入的文件');
    return;
  }

  importLoading.value = true;
  
  try {
    const result = await proxy.$api.dataset.importData(currentTable.value.tableName, selectedFile.value, appId);
    if (result.success) {
      proxy.$modal.msgSuccess(t('page.tables.import_success'));
      importDialogVisible.value = false;
      // 重新加载数据
      selectTableData(currentTable.value);
    } else {
      proxy.$modal.msgError(result.message || t('page.tables.import_failed'));
    }
  } catch (error) {
    console.error('导入失败:', error);
    proxy.$modal.msgError(error.message || t('page.tables.import_failed'));
  } finally {
    importLoading.value = false;
  }
}

onMounted(() => {
  window.onresize = function windowResize() {
    calWidthAndHeight();
  };
  calWidthAndHeight();
});

const tableWidth = ref(0);

function calWidthAndHeight() {
  let sidebar = document.getElementById('table-header');
  tableWidth.value = sidebar ? sidebar.offsetWidth : 0;
}


</script>