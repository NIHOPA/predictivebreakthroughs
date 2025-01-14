read_configs = function(config_path="./config.ini") {
  confs = strsplit(readLines(config_path), "=", fixed=T)
  configs = list()
  for (lineNum in 1:length(confs)) {
    configs[[lineNum]] = trimws(paste(confs[[lineNum]][2:length(confs[[lineNum]])], sep="="))
    names(configs)[lineNum] = trimws(confs[[lineNum]][1])
  }
  return(configs)
}

explode = function(df, key_col=1, val_col=2, key_delim=" ", val_delim=";", verbose=F, recast=F) {
  if (!is.data.frame(df)) stop("Please input a data frame with more than one column.")
  if (ncol(df) < 2) stop("Please input more than one column.")
  if (is.character(key_col)) {
    key_col = which(colnames(df) == key_col)
    if (length(key_col) == 0) stop("Key column name not found.")
  }
  if (is.character(val_col)) {
    val_col = which(colnames(df) == val_col)
    if (length(val_col) == 0) stop("Val column name not found.")
  }
  if (verbose) print("Splitting.")
  # Cast empty strings as NA, so that it propagates into the val_vect
  df[which(df[,val_col] == ""), val_col] <- as.character(NA)
  val_list <- strsplit(df[,val_col], val_delim, fixed=T)
  names(val_list) <- paste0(as.character(df[,key_col]), key_delim)
  
  # Unlist to a vector
  if (verbose) print("Unlisting.")
  val_vect = unlist(val_list)
  
  # Memory management for big datasets
  remove(val_list)
  if (recast) {
    temp_names = names(val_vect)
    val_vect <- type.convert(val_vect, as.is=T)
    names(val_vect) = temp_names
    remove(temp_names)
  }
  
  if (verbose) print(paste("Number of NAs in values:", sum(is.na(val_vect))))
  
  # Retreive keys
  key_vect = sapply(strsplit(names(val_vect), key_delim, fixed=T), function(x) x[1])
  
  # Detect class of key column and re-cast key_vect to match
  if (verbose) print("Casting keys.")
  cast_func = get(paste0("as.", typeof(df[,key_col])))
  key_vect <- cast_func(key_vect)
  
  # Get match indexes
  if (verbose) print("Finding key matches.")
  matches = match(key_vect, df[,key_col])
  
  # Build first col of data frame
  if (verbose) print("Building data frame.")
  if (key_col == 1) {
    df_out = data.frame(key_vect, stringsAsFactors = F)
  } else if (val_col == 1) {
    df_out = data.frame(val_vect, stringsAsFactors = F)
  } else {
    df_out = data.frame(df[matches, 1], stringsAsFactors = F)
  }
  
  # Build other cols of data frame
  for (i in 2:ncol(df)) {
    if (key_col == i) {
      df_out = cbind(df_out, key_vect, stringsAsFactors = F)
    } else if (val_col == i) {
      df_out = cbind(df_out, val_vect, stringsAsFactors = F)
    } else {
      df_out = cbind(df_out, df[matches, i], stringsAsFactors = F)
    }
  }
  
  colnames(df_out) = colnames(df)
  
  return(df_out)
}
