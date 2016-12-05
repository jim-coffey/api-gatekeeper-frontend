$(function(undefined) {
  // Copy Button
  (function() {
    var copyButton = $('#copy-to-clip');

    function copyTextToClipboard(text) {
      var textArea = document.createElement("textarea");
      textArea.style.position = 'fixed';
      textArea.style.top = 0;
      textArea.style.left = 0;
      textArea.style.width = '2em';
      textArea.style.height = '2em';
      textArea.style.padding = 0;
      textArea.style.border = 'none';
      textArea.style.outline = 'none';
      textArea.style.boxShadow = 'none';
      textArea.style.background = 'transparent';
      textArea.value = text;

      document.body.appendChild(textArea);

      textArea.select();

      try {
        var successful = document.execCommand('copy');
      } catch (e) {
        // allow failure - still want to remove textArea
        // test if we should even display the button later
      }

      document.body.removeChild(textArea);
    }

    if (copyButton.length) {
      copyButton.on('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        copyTextToClipboard( copyButton.data('clip-text') );
      });
    }
  })();

  // DataTables
  (function() {
    function getAllFilters (table) {
      return $('*[data-datatable-filter="' + table + '"]');
    }

    function getTableFilters (table) {
      return $('*[data-datatable-filter="' + table + '"]')
               .not('[data-datatable-column-filter]');
    };

    function getTableFilterValues (filters) {
      var filterValues = [];

      $.each(filters, function (index, filter) {
        var $filter = $(filter);
        var inputVal;

        inputVal = $(filter).val();
        if (inputVal) {
          filterValues.push(inputVal);
        }
      });

      return filterValues;
    }

    function buildFilter (filterValues) {
      return filterValues.join(' ');
    }

    function searchTable(table) {
      var dataTable = $('#' + table).DataTable();
      var filterValue = buildFilter( getTableFilterValues( getTableFilters(table) ) );
      dataTable
        .search(filterValue)
        .draw();
    }

    function searchColumn(filter, table, column) {
      var dataTable = $('#' + table).DataTable();
      var filterValue = "";

      if (filter.selectedIndex) {
        filterValue = filter.options[filter.selectedIndex].text;
         if (filterValue) {
          filterValue = '^' + filterValue + '$'; // Filter by exact match
        }
      }

      dataTable
        .columns(column)
        .search(filterValue, true, false)
        .draw();
    }

    function search (e) {
      var table = $(this).data('datatable-filter');
      var column = $(this).data('datatable-column-filter');

      if (column) {
        searchColumn(this, table, parseInt(column));
      } else {
        searchTable(table);
      }
    }

    function listenToFilter (index, filter) {
      var $filter = $(filter);

      if (filter.options) {
        $filter.on('change', search);
      } else if ($filter.is('input')) {
        $filter.on('keydown', search);
        $filter.on('keypress', search);
        $filter.on('keyup', search);
      }
    }

    var DEVELOPER_TABLE = 'developer-table';
    var dataTables = [
      DEVELOPER_TABLE
    ];

    // For all dataTables on the page
    $.each(dataTables, function (index, table) {
      // Hook in to custom filters
      var filters = getAllFilters(table);
      $.each(filters, listenToFilter);
      // Instantiate the dataTable
      $('#' + table).DataTable();
    });

    // Chosen-ify the multi-select
    $("#filter").chosen();
  })();
});
