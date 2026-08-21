-- public.barcode_data definition

-- Drop table

-- DROP TABLE public.barcode_data;

CREATE TABLE public.barcode_data (
	barcode varchar(255) NOT NULL,
	line_id varchar(255) NOT NULL,
	api_result text NULL,
	cre_date timestamp(6) NOT NULL,
	last_status_date timestamp(6) NULL,
	status varchar(255) NOT NULL,
	CONSTRAINT barcode_data_pkey PRIMARY KEY (barcode, line_id),
	CONSTRAINT barcode_data_status_check CHECK (((status)::text = ANY ((ARRAY['NEW'::character varying, 'SENT'::character varying, 'ERROR'::character varying])::text[])))
);


-- public.line_info definition

-- Drop table

-- DROP TABLE public.line_info;

CREATE TABLE public.line_info (
	line_id varchar(255) NOT NULL,
	last_status_date timestamp(6) NOT NULL,
	line_name varchar(255) NOT NULL,
	status varchar(255) NOT NULL,
	CONSTRAINT line_info_pkey PRIMARY KEY (line_id),
	CONSTRAINT line_info_status_check CHECK (((status)::text = ANY ((ARRAY['RUN'::character varying, 'PASSIVE'::character varying, 'STOP'::character varying])::text[])))
);


-- public.line_log definition

-- Drop table

-- DROP TABLE public.line_log;

CREATE TABLE public.line_log (
	line_id varchar(255) NOT NULL,
	seq_no int8 NOT NULL,
	proc_date timestamp(6) NOT NULL,
	status varchar(255) NOT NULL,
	CONSTRAINT line_log_pkey PRIMARY KEY (line_id, seq_no),
	CONSTRAINT line_log_status_check CHECK (((status)::text = ANY ((ARRAY['RUN'::character varying, 'PASSIVE'::character varying, 'STOP'::character varying])::text[])))
);


-- public.plc_info definition

-- Drop table

-- DROP TABLE public.plc_info;

CREATE TABLE public.plc_info (
	plc_id varchar(255) NOT NULL,
	last_status_date timestamp(6) NOT NULL,
	plc_ip varchar(255) NOT NULL,
	status varchar(255) NOT NULL,
	CONSTRAINT plc_info_pkey PRIMARY KEY (plc_id),
	CONSTRAINT plc_info_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PASSIVE'::character varying, 'NOCOMM'::character varying])::text[])))
);


-- public.plc_log definition

-- Drop table

-- DROP TABLE public.plc_log;

CREATE TABLE public.plc_log (
	plc_id varchar(255) NOT NULL,
	seq_no int8 NOT NULL,
	proc_date timestamp(6) NOT NULL,
	status varchar(255) NOT NULL,
	CONSTRAINT plc_log_pkey PRIMARY KEY (plc_id, seq_no),
	CONSTRAINT plc_log_status_check CHECK (((status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'PASSIVE'::character varying, 'NOCOMM'::character varying])::text[])))
);