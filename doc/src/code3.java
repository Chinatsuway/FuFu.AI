    private void addEmptyCell() {
        View emptyView = LayoutInflater.from(getContext())
                .inflate(R.layout.calendar_day_cell, calendarGrid, false);
        TextView dayNumber = emptyView.findViewById(R.id.dayNumber);
        dayNumber.setText("");
        dayNumber.setVisibility(View.INVISIBLE);
        emptyView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.empty_cell_color));
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = 0; // 使用权重控制高度
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // 行权重为1
        emptyView.setLayoutParams(params);

        calendarGrid.addView(emptyView);
    }